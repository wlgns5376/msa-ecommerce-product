package com.commerce.product.domain.service.impl;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.exception.LockAcquisitionException;
import com.commerce.product.domain.model.DistributedLock;
import com.commerce.product.domain.model.ProductOption;
import com.commerce.product.domain.model.SkuMapping;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.SagaRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;
import com.commerce.product.domain.service.saga.BundleReservationSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class StockAvailabilityServiceImpl implements StockAvailabilityService {
    
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final LockRepository lockRepository;
    private final SagaRepository sagaRepository;
    private final DomainEventPublisher eventPublisher;
    private final BundleReservationSagaOrchestrator bundleReservationSagaOrchestrator;
    private static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(5);
    
    @Override
    public boolean checkSingleOption(String skuId, int requestedQuantity) {
        if (requestedQuantity < 0) {
            throw new IllegalArgumentException(
                    String.format("Requested quantity cannot be negative: %d for SKU: %s", requestedQuantity, skuId));
        }
        
        if (requestedQuantity == 0) {
            return true;
        }
        
        int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
        
        if (availableQuantity < 0) {
            throw new IllegalStateException(
                    String.format("Available quantity cannot be negative: %d for SKU: %s", availableQuantity, skuId));
        }
        
        boolean isAvailable = availableQuantity >= requestedQuantity;
        
        if (!isAvailable) {
            log.debug("Insufficient stock for SKU: {}. Available: {}, Requested: {}", 
                    skuId, availableQuantity, requestedQuantity);
        }
        
        return isAvailable;
    }
    
    @Override
    public boolean checkBundleOption(ProductOption bundleOption, int requestedQuantity) {
        if (!bundleOption.isBundle()) {
            return checkSingleOption(bundleOption.getSingleSkuId(), requestedQuantity);
        }
        
        for (Map.Entry<String, Integer> entry : bundleOption.getSkuMapping().mappings().entrySet()) {
            String skuId = entry.getKey();
            int requiredQuantity = entry.getValue() * requestedQuantity;
            
            if (!checkSingleOption(skuId, requiredQuantity)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public CompletableFuture<Map<String, Boolean>> checkMultipleOptions(
            List<ProductOption> options, Map<String, Integer> quantities) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Boolean> results = new ConcurrentHashMap<>();
            
            List<CompletableFuture<Void>> futures = options.stream()
                    .map(option -> CompletableFuture.runAsync(() -> {
                        String optionName = option.getName();
                        Integer requestedQuantity = quantities.get(optionName);
                        
                        if (requestedQuantity == null) {
                            results.put(optionName, false);
                            return;
                        }
                        
                        boolean available = option.isBundle() 
                                ? checkBundleOption(option, requestedQuantity)
                                : checkSingleOption(option.getSingleSkuId(), requestedQuantity);
                        
                        results.put(optionName, available);
                    }))
                    .collect(Collectors.toList());
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            return results;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> reserveStock(String skuId, int quantity, String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<DistributedLock> lockOpt = lockRepository.acquireLock(
                "stock:" + skuId, 
                DEFAULT_LEASE_DURATION, 
                DEFAULT_WAIT_TIMEOUT
            );
            
            if (lockOpt.isEmpty()) {
                log.warn("Failed to acquire lock for SKU: {}", skuId);
                throw new LockAcquisitionException("Unable to acquire lock for SKU: " + skuId);
            }
            
            DistributedLock lock = lockOpt.get();
            
            try {
                int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
                
                if (availableQuantity < quantity) {
                    log.info("Insufficient stock for SKU: {}. Available: {}, Requested: {}", 
                            skuId, availableQuantity, quantity);
                    return false;
                }
                
                String reservationId = inventoryRepository.reserveStock(skuId, quantity, orderId);
                log.info("Stock reserved. SKU: {}, Quantity: {}, Order: {}, Reservation: {}", 
                        skuId, quantity, orderId, reservationId);
                
                return true;
                
            } finally {
                lockRepository.releaseLock(lock);
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> reserveBundleStock(
            ProductOption bundleOption, int quantity, String orderId) {
        
        if (!bundleOption.isBundle()) {
            return reserveStock(bundleOption.getSingleSkuId(), quantity, orderId);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            
            Map<String, Integer> requiredQuantities = new HashMap<>();
            List<String> skuIds = new ArrayList<>();
            
            for (Map.Entry<String, Integer> entry : bundleOption.getSkuMapping().mappings().entrySet()) {
                String skuId = entry.getKey();
                int requiredQuantity = entry.getValue() * quantity;
                requiredQuantities.put(skuId, requiredQuantity);
                skuIds.add(skuId);
            }
            
            // 데드락 방지를 위해 SKU ID 순서대로 정렬
            Collections.sort(skuIds);
            
            Map<String, DistributedLock> locks = new LinkedHashMap<>();
            Map<String, String> reservations = new LinkedHashMap<>();
            
            try {
                // 모든 락 획득
                for (String skuId : skuIds) {
                    Optional<DistributedLock> lockOpt = lockRepository.acquireLock(
                        "stock:" + skuId, 
                        DEFAULT_LEASE_DURATION, 
                        DEFAULT_WAIT_TIMEOUT
                    );
                    
                    if (lockOpt.isEmpty()) {
                        log.warn("Failed to acquire lock for SKU: {} in bundle", skuId);
                        throw new LockAcquisitionException("Unable to acquire lock for bundle SKU: " + skuId);
                    }
                    locks.put(skuId, lockOpt.get());
                }
                
                // 재고 가용성 체크
                for (String skuId : skuIds) {
                    int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
                    int requiredQuantity = requiredQuantities.get(skuId);
                    
                    if (availableQuantity < requiredQuantity) {
                        log.info("Insufficient stock for bundle SKU: {}. Available: {}, Required: {}", 
                                skuId, availableQuantity, requiredQuantity);
                        // 보상 트랜잭션 실행
                        compensateReservations(reservations);
                        return false;
                    }
                }
                
                // 모든 SKU 예약
                for (String skuId : skuIds) {
                    int requiredQuantity = requiredQuantities.get(skuId);
                    String reservationId = inventoryRepository.reserveStock(skuId, requiredQuantity, orderId);
                    reservations.put(skuId, reservationId);
                    
                    log.info("Bundle stock reserved. SKU: {}, Quantity: {}, Order: {}, Reservation: {}", 
                            skuId, requiredQuantity, orderId, reservationId);
                }
                
                return true;
                
            } catch (Exception e) {
                log.error("Error during bundle stock reservation", e);
                compensateReservations(reservations);
                return false;
                
            } finally {
                // 역순으로 락 해제
                releaseLocks(locks);
            }
        });
    }
    
    private void compensateReservations(Map<String, String> reservations) {
        for (Map.Entry<String, String> entry : reservations.entrySet()) {
            String skuId = entry.getKey();
            String reservationId = entry.getValue();
            
            try {
                inventoryRepository.releaseReservation(reservationId);
                log.info("Compensated reservation. SKU: {}, Reservation: {}", skuId, reservationId);
            } catch (Exception e) {
                log.error("Failed to compensate reservation. SKU: {}, Reservation: {}", 
                        skuId, reservationId, e);
            }
        }
    }
    
    @Override
    public void releaseReservation(String reservationId) {
        inventoryRepository.releaseReservation(reservationId);
        log.info("Released reservation: {}", reservationId);
    }
    
    @Override
    public int getAvailableQuantity(String skuId) {
        return inventoryRepository.getAvailableQuantity(skuId);
    }
    
    @Override
    public Map<String, Integer> getBundleAvailableQuantity(ProductOption bundleOption) {
        Map<String, Integer> quantities = new HashMap<>();
        
        for (String skuId : bundleOption.getSkuMapping().mappings().keySet()) {
            quantities.put(skuId, getAvailableQuantity(skuId));
        }
        
        return quantities;
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
    
    @Override
    public CompletableFuture<AvailabilityResult> checkProductOptionAvailability(String optionId) {
        return CompletableFuture.supplyAsync(() -> {
            ProductOption option = productRepository.findOptionById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("Option not found: " + optionId));
            return option;
        }).thenCompose(option -> {
            if (!option.isBundle()) {
                // 단일 SKU 옵션
                String skuId = option.getSingleSkuId();
                int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
                AvailabilityResult result = availableQuantity > 0 
                    ? AvailabilityResult.available(availableQuantity)
                    : AvailabilityResult.unavailable();
                return CompletableFuture.completedFuture(result);
            }
            
            // 묶음 옵션
            return checkBundleAvailability(option.getSkuMapping())
                    .thenApply(bundleResult -> bundleResult.isAvailable() 
                        ? AvailabilityResult.available(bundleResult.availableSets())
                        : AvailabilityResult.unavailable());
        });
    }
    
    @Override
    public CompletableFuture<AvailabilityResult> checkSingleSkuAvailability(String skuId) {
        return CompletableFuture.supplyAsync(() -> {
            // N+1 문제를 방지하기 위해 SKU ID를 직접 받아 처리
            int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
            AvailabilityResult result = availableQuantity > 0 
                ? AvailabilityResult.available(availableQuantity)
                : AvailabilityResult.unavailable();
            return result;
        });
    }
    
    @Override
    public CompletableFuture<BundleAvailabilityResult> checkBundleAvailability(SkuMapping skuMapping) {
        return CompletableFuture.supplyAsync(() -> {
            // 분산 락을 사용하여 번들 재고 확인의 원자성 보장
            List<String> skuIds = new ArrayList<>(skuMapping.mappings().keySet());
            Collections.sort(skuIds); // 데드락 방지
            
            String lockKey = "bundle-stock-check:" + String.join(":", skuIds);
            Optional<DistributedLock> lockOpt = lockRepository.acquireLock(lockKey, DEFAULT_LEASE_DURATION, DEFAULT_WAIT_TIMEOUT);
            
            if (lockOpt.isEmpty()) {
                throw new LockAcquisitionException("Unable to acquire lock for bundle stock check");
            }
            
            DistributedLock lock = lockOpt.get();
            
            try {
                List<BundleAvailabilityResult.SkuAvailabilityDetail> details = new ArrayList<>();
                int minAvailableSets = Integer.MAX_VALUE;
                
                for (Map.Entry<String, Integer> entry : skuMapping.mappings().entrySet()) {
                    String skuId = entry.getKey();
                    int requiredQuantity = entry.getValue();
                    int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
                    int availableSets = requiredQuantity > 0 ? availableQuantity / requiredQuantity : 0;
                    
                    details.add(new BundleAvailabilityResult.SkuAvailabilityDetail(
                        skuId, requiredQuantity, availableQuantity, availableSets
                    ));
                    
                    minAvailableSets = Math.min(minAvailableSets, availableSets);
                }
                
                return minAvailableSets > 0 
                    ? BundleAvailabilityResult.available(minAvailableSets, details)
                    : BundleAvailabilityResult.unavailable(details);
                    
            } finally {
                lockRepository.releaseLock(lock);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> reserveBundleStock(SkuMapping skuMapping, String reservationId) {
        // 임시 orderId 생성 (실제로는 주문 서비스에서 전달받아야 함)
        String orderId = "ORDER-" + UUID.randomUUID().toString();
        
        return bundleReservationSagaOrchestrator.execute(orderId, skuMapping, 1, reservationId);
    }
}