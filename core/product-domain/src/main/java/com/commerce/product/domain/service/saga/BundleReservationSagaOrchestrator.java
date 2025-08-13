package com.commerce.product.domain.service.saga;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.event.BundleReservationCompletedEvent;
import com.commerce.product.domain.event.BundleReservationFailedEvent;
import com.commerce.product.domain.exception.LockAcquisitionException;
import com.commerce.product.domain.model.DistributedLock;
import com.commerce.product.domain.model.SkuMapping;
import com.commerce.product.domain.model.saga.BundleReservationSaga;
import com.commerce.product.domain.model.saga.SagaStep;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.SagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class BundleReservationSagaOrchestrator {
    
    private final InventoryRepository inventoryRepository;
    private final LockRepository lockRepository;
    private final SagaRepository sagaRepository;
    private final DomainEventPublisher eventPublisher;
    
    private static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(5);
    
    public CompletableFuture<Void> execute(String orderId, SkuMapping skuMapping, int quantity, String reservationId) {
        return CompletableFuture.runAsync(() -> {
            // Saga 생성 및 저장
            BundleReservationSaga saga = new BundleReservationSaga(reservationId, orderId, skuMapping, quantity);
            sagaRepository.save(saga);
            
            try {
                saga.start();
                sagaRepository.update(saga);
                
                // 각 SKU에 대한 예약 실행
                executeReservationSteps(saga);
                
                if (saga.getStatus().isTerminal()) {
                    if (saga.getStatus() == com.commerce.product.domain.model.saga.SagaStatus.COMPLETED) {
                        eventPublisher.publish(new BundleReservationCompletedEvent(
                            saga.getReservationId(), 
                            saga.getOrderId(), 
                            saga.getSkuMapping()
                        ));
                    } else {
                        eventPublisher.publish(new BundleReservationFailedEvent(
                            saga.getReservationId(), 
                            saga.getOrderId(), 
                            saga.getFailureReason()
                        ));
                    }
                }
                
            } catch (Exception e) {
                log.error("Unexpected error during saga execution", e);
                compensate(saga);
                saga.fail("Unexpected error: " + e.getMessage());
                sagaRepository.update(saga);
                
                eventPublisher.publish(new BundleReservationFailedEvent(
                    saga.getReservationId(), 
                    saga.getOrderId(), 
                    e.getMessage()
                ));
                
                throw new CompletionException(e);
            }
        });
    }
    
    private void executeReservationSteps(BundleReservationSaga saga) {
        Map<String, Integer> requiredQuantities = calculateRequiredQuantities(saga);
        List<String> sortedSkuIds = new ArrayList<>(requiredQuantities.keySet());
        Collections.sort(sortedSkuIds); // 데드락 방지를 위한 정렬
        
        Map<String, DistributedLock> locks = new LinkedHashMap<>();
        Map<String, String> reservations = new LinkedHashMap<>();
        
        try {
            // 모든 락 획득
            acquireAllLocks(sortedSkuIds, locks);
            
            // 재고 가용성 체크 및 예약
            for (String skuId : sortedSkuIds) {
                try {
                    int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
                    int requiredQuantity = requiredQuantities.get(skuId);
                    
                    if (availableQuantity < requiredQuantity) {
                        String errorMessage = String.format(
                            "Insufficient stock for SKU %s. Available: %d, Required: %d", 
                            skuId, availableQuantity, requiredQuantity
                        );
                        saga.recordStepFailure(skuId, errorMessage);
                        sagaRepository.update(saga);
                        
                        // 보상 트랜잭션 실행
                        compensateReservations(saga, reservations);
                        break;
                    }
                    
                    // 재고 예약
                    String reservationIdForSku = inventoryRepository.reserveStock(
                        skuId, requiredQuantity, saga.getOrderId()
                    );
                    reservations.put(skuId, reservationIdForSku);
                    
                    // 성공 기록
                    saga.recordStepSuccess(skuId, reservationIdForSku);
                    sagaRepository.update(saga);
                    
                    log.info("Reserved stock for SKU: {}, Quantity: {}, ReservationId: {}", 
                        skuId, requiredQuantity, reservationIdForSku);
                    
                } catch (Exception e) {
                    log.error("Failed to reserve stock for SKU: {}", skuId, e);
                    saga.recordStepFailure(skuId, e.getMessage());
                    sagaRepository.update(saga);
                    
                    compensateReservations(saga, reservations);
                    break;
                }
            }
            
            // Saga가 이미 recordStepSuccess를 통해 자동으로 complete 되므로
            // 추가적인 update는 필요하지 않음
            
        } finally {
            releaseLocks(locks);
        }
    }
    
    private Map<String, Integer> calculateRequiredQuantities(BundleReservationSaga saga) {
        return saga.getSkuMapping().mappings().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() * saga.getQuantity()
                ));
    }
    
    private void acquireAllLocks(List<String> skuIds, Map<String, DistributedLock> locks) {
        for (String skuId : skuIds) {
            Optional<DistributedLock> lockOpt = lockRepository.acquireLock(
                "stock:" + skuId, 
                DEFAULT_LEASE_DURATION, 
                DEFAULT_WAIT_TIMEOUT
            );
            
            if (lockOpt.isEmpty()) {
                log.warn("Failed to acquire lock for SKU: {}", skuId);
                throw new LockAcquisitionException("Unable to acquire lock for SKU: " + skuId);
            }
            
            locks.put(skuId, lockOpt.get());
        }
    }
    
    private void compensateReservations(BundleReservationSaga saga, Map<String, String> reservations) {
        for (Map.Entry<String, String> entry : reservations.entrySet()) {
            String skuId = entry.getKey();
            String reservationId = entry.getValue();
            
            try {
                inventoryRepository.releaseReservation(reservationId);
                saga.recordCompensation(skuId);
                sagaRepository.update(saga);
                
                log.info("Compensated reservation for SKU: {}, ReservationId: {}", skuId, reservationId);
            } catch (Exception e) {
                log.error("Failed to compensate reservation for SKU: {}, ReservationId: {}", 
                    skuId, reservationId, e);
            }
        }
        
        // 보상이 완료되면 saga를 FAILED 상태로 업데이트
        if (saga.getStatus() != com.commerce.product.domain.model.saga.SagaStatus.FAILED) {
            saga.fail("Compensated due to reservation failure");
            sagaRepository.update(saga);
        }
    }
    
    public void compensate(BundleReservationSaga saga) {
        if (!saga.getStatus().canCompensate()) {
            log.warn("Cannot compensate saga in status: {}", saga.getStatus());
            return;
        }
        
        List<SagaStep> compensatableSteps = saga.getCompensatableSteps();
        
        for (SagaStep step : compensatableSteps) {
            try {
                String reservationId = step.getCompensationData();
                if (reservationId != null) {
                    inventoryRepository.releaseReservation(reservationId);
                    saga.recordCompensation(step.getStepId());
                    sagaRepository.update(saga);
                    
                    log.info("Compensated step: {}", step.getStepId());
                }
            } catch (Exception e) {
                log.error("Failed to compensate step: {}", step.getStepId(), e);
            }
        }
    }
    
    private void releaseLocks(Map<String, DistributedLock> locks) {
        List<String> reverseSkuIds = new ArrayList<>(locks.keySet());
        Collections.reverse(reverseSkuIds);
        
        for (String skuId : reverseSkuIds) {
            DistributedLock lock = locks.get(skuId);
            if (lock != null) {
                try {
                    lockRepository.releaseLock(lock);
                } catch (Exception e) {
                    log.error("Error releasing lock for SKU: {}", skuId, e);
                }
            }
        }
    }
}