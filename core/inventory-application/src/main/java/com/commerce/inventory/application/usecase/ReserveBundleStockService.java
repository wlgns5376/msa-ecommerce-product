package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.BundleReservationResponse;
import com.commerce.inventory.application.port.in.BundleReservationStatus;
import com.commerce.inventory.application.port.in.ReserveBundleStockCommand;
import com.commerce.inventory.application.port.in.ReserveBundleStockUseCase;
import com.commerce.inventory.application.port.in.SkuReservationStatus;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.repository.ReservationRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReserveBundleStockService implements ReserveBundleStockUseCase {
    
    @Value("${inventory.reservation.default-ttl-seconds:900}")
    private int defaultTtlSeconds; // 15분
    
    private final LoadInventoryPort loadInventoryPort;
    private final SaveInventoryPort saveInventoryPort;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    private final Validator validator;
    
    @Override
    @Transactional
    public BundleReservationResponse execute(ReserveBundleStockCommand command) {
        validateCommand(command);
        
        String sagaId = command.getSagaId();
        
        try {
            // 1. 모든 개별 예약 요청 계산
            List<SkuReservationRequest> skuRequests = parseSkuRequests(command);
            if (skuRequests.isEmpty()) {
                return createSuccessResponse(sagaId, command.getOrderId(), Collections.emptyList());
            }

            // 2. 총 필요 수량 집계
            Map<SkuId, Quantity> totalRequiredQuantities = calculateTotalRequiredQuantities(skuRequests);

            // 3. 재고 조회 및 검증
            Map<SkuId, Inventory> inventoryMap = loadAndValidateInventories(totalRequiredQuantities);

            // 4. 예약 생성 및 저장
            List<Reservation> savedReservations = createAndSaveReservations(command, skuRequests, inventoryMap);
            
            // 5. 성공 응답 생성
            return createSuccessResponse(sagaId, command.getOrderId(), savedReservations);
            
        } catch (InsufficientStockException | InvalidInventoryException | ArithmeticException e) {
            log.error("번들 재고 예약 실패: sagaId={}, error={}", sagaId, e.getMessage(), e);
            
            // 실패 응답 생성 (트랜잭션이 자동으로 롤백됨)
            return createFailureResponse(sagaId, command.getOrderId(), e.getMessage());
        }
    }

    private List<SkuReservationRequest> parseSkuRequests(ReserveBundleStockCommand command) {
        return command.getBundleItems().stream()
            .flatMap(bundleItem -> bundleItem.getSkuMappings().stream()
                .map(skuMapping -> new SkuReservationRequest(
                    SkuId.of(skuMapping.getSkuId()),
                    Quantity.of(Math.multiplyExact(skuMapping.getQuantity(), bundleItem.getQuantity()))
                ))
            )
            .filter(request -> request.quantity().value() > 0)
            .collect(Collectors.toList());
    }

    private Map<SkuId, Quantity> calculateTotalRequiredQuantities(List<SkuReservationRequest> skuRequests) {
        return skuRequests.stream()
            .collect(Collectors.toMap(
                SkuReservationRequest::skuId,
                SkuReservationRequest::quantity,
                (q1, q2) -> Quantity.of(Math.addExact(q1.value(), q2.value()))
            ));
    }

    private Map<SkuId, Inventory> loadAndValidateInventories(Map<SkuId, Quantity> totalRequiredQuantities) {
        // 모든 재고 정보 한 번에 조회
        List<SkuId> allSkuIds = new ArrayList<>(totalRequiredQuantities.keySet());
        Map<SkuId, Inventory> inventoryMap = loadInventoryPort.loadAllByIds(allSkuIds);
        
        // 존재하지 않는 SKU 검증
        validateInventoryExists(allSkuIds, inventoryMap);
        
        // 모든 항목에 대한 재고 가용성 사전 확인
        validateInventoryAvailability(totalRequiredQuantities, inventoryMap);
        
        return inventoryMap;
    }

    private void validateInventoryExists(List<SkuId> requiredSkuIds, Map<SkuId, Inventory> inventoryMap) {
        if (inventoryMap.size() != requiredSkuIds.size()) {
            Set<SkuId> missingSkuIdSet = new HashSet<>(requiredSkuIds);
            missingSkuIdSet.removeAll(inventoryMap.keySet());
            String missingSkuIds = missingSkuIdSet.stream()
                    .map(SkuId::value)
                    .collect(Collectors.joining(", "));
            throw new InvalidInventoryException("다음 SKU에 대한 재고 정보를 찾을 수 없습니다: " + missingSkuIds);
        }
    }

    private void validateInventoryAvailability(Map<SkuId, Quantity> totalRequiredQuantities, Map<SkuId, Inventory> inventoryMap) {
        for (Map.Entry<SkuId, Quantity> entry : totalRequiredQuantities.entrySet()) {
            SkuId skuId = entry.getKey();
            Quantity requiredQuantity = entry.getValue();
            Inventory inventory = inventoryMap.get(skuId);
            
            if (!inventory.canReserve(requiredQuantity)) {
                throw new InsufficientStockException(
                    String.format("재고가 부족합니다. SKU: %s, 가용 재고: %d, 요청 수량: %d",
                        skuId.value(),
                        inventory.getAvailableQuantity().value(),
                        requiredQuantity.value())
                );
            }
        }
    }

    private List<Reservation> createAndSaveReservations(
        ReserveBundleStockCommand command,
        List<SkuReservationRequest> skuRequests,
        Map<SkuId, Inventory> inventoryMap
    ) {
        Set<Inventory> modifiedInventories = new HashSet<>();
        List<Reservation> reservationsToSave = new ArrayList<>();
        
        int ttlSeconds = Optional.ofNullable(command.getTtlSeconds()).orElse(defaultTtlSeconds);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusSeconds(ttlSeconds);
        
        for (SkuReservationRequest request : skuRequests) {
            Inventory inventory = inventoryMap.get(request.skuId());
            
            // 재고 예약 (도메인 객체가 재고 확인 및 예외 처리 담당)
            ReservationId reservationId = inventory.reserve(request.quantity());
            
            // 예약 엔티티 생성
            Reservation reservation = Reservation.create(
                reservationId,
                inventory.getSkuId(),
                request.quantity(),
                command.getOrderId(),
                expiresAt,
                now
            );
            
            reservationsToSave.add(reservation);
            modifiedInventories.add(inventory);
        }
        
        // 예약 정보 일괄 저장
        List<Reservation> savedReservations = reservationRepository.saveAll(reservationsToSave);
        
        // 변경된 재고 정보 일괄 저장
        saveInventoryPort.saveAll(modifiedInventories);
        
        return savedReservations;
    }
    
    private BundleReservationResponse createSuccessResponse(
        String sagaId,
        String orderId,
        List<Reservation> savedReservations
    ) {
        List<BundleReservationResponse.SkuReservation> skuReservations = savedReservations.stream()
            .map(reservation -> BundleReservationResponse.SkuReservation.builder()
                .skuId(reservation.getSkuId().value())
                .reservationId(reservation.getId().value())
                .quantity(reservation.getQuantity().value())
                .expiresAt(reservation.getExpiresAt())
                .status(SkuReservationStatus.ACTIVE)
                .build())
            .collect(Collectors.toList());
        
        return BundleReservationResponse.builder()
            .sagaId(sagaId)
            .orderId(orderId)
            .status(BundleReservationStatus.COMPLETED)
            .skuReservations(skuReservations)
            .failureReason(null)
            .build();
    }
    
    private BundleReservationResponse createFailureResponse(
        String sagaId,
        String orderId,
        String failureReason
    ) {
        return BundleReservationResponse.builder()
            .sagaId(sagaId)
            .orderId(orderId)
            .status(BundleReservationStatus.FAILED)
            .skuReservations(Collections.emptyList())
            .failureReason(failureReason)
            .build();
    }
    
    private void validateCommand(ReserveBundleStockCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("예약 요청이 null일 수 없습니다");
        }
        Set<ConstraintViolation<ReserveBundleStockCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    // 내부 레코드: SKU 예약 요청
    private record SkuReservationRequest(
        SkuId skuId,
        Quantity quantity
    ) {}
}