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
    
    private static final int DEFAULT_TTL_SECONDS = 900; // 15분
    
    private final LoadInventoryPort loadInventoryPort;
    private final SaveInventoryPort saveInventoryPort;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    private final Validator validator;
    
    @Override
    @Transactional
    public BundleReservationResponse execute(ReserveBundleStockCommand command) {
        validateCommand(command);
        
        String sagaId = command.getReservationId();
        List<ReservedItem> reservedItems = new ArrayList<>();
        Set<Inventory> modifiedInventories = new HashSet<>();
        
        try {
            // 1. SKU별 총 필요 수량 계산
            Map<SkuId, Integer> totalRequiredQuantities = command.getBundleItems().stream()
                .flatMap(item -> item.getSkuMappings().stream()
                    .map(mapping -> new AbstractMap.SimpleEntry<>(
                        new SkuId(mapping.getSkuId()),
                        mapping.getQuantity() * item.getQuantity()
                    )))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    Integer::sum
                ));
            
            if (totalRequiredQuantities.isEmpty()) {
                return createSuccessResponse(sagaId, command.getOrderId(), Collections.emptyList());
            }

            // 2. 모든 재고 정보 한 번에 조회
            List<SkuId> allSkuIds = new ArrayList<>(totalRequiredQuantities.keySet());
            Map<SkuId, Inventory> inventoryMap = loadInventoryPort.loadAllByIds(allSkuIds);
            
            // 3. 존재하지 않는 SKU 검증
            if (inventoryMap.size() != allSkuIds.size()) {
                Set<SkuId> foundSkuIds = inventoryMap.keySet();
                String missingSkuIds = allSkuIds.stream()
                        .filter(id -> !foundSkuIds.contains(id))
                        .map(SkuId::value)
                        .collect(Collectors.joining(", "));
                throw new InvalidInventoryException("다음 SKU에 대한 재고 정보를 찾을 수 없습니다: " + missingSkuIds);
            }
            
            // 4. 모든 항목에 대한 재고 가용성 사전 확인
            for (Map.Entry<SkuId, Integer> entry : totalRequiredQuantities.entrySet()) {
                SkuId skuId = entry.getKey();
                Integer requiredQuantity = entry.getValue();
                Inventory inventory = inventoryMap.get(skuId);
                
                if (!inventory.canReserve(Quantity.of(requiredQuantity))) {
                    throw new InsufficientStockException(
                        String.format("재고가 부족합니다. SKU: %s, 가용 재고: %d, 요청 수량: %d",
                            skuId.value(),
                            inventory.getAvailableQuantity().value(),
                            requiredQuantity)
                    );
                }
            }
            
            // 5. 모든 검증 통과 후 재고 예약 실행
            for (ReserveBundleStockCommand.BundleItem bundleItem : command.getBundleItems()) {
                for (ReserveBundleStockCommand.SkuMapping skuMapping : bundleItem.getSkuMappings()) {
                    int requiredQuantity = skuMapping.getQuantity() * bundleItem.getQuantity();
                    if (requiredQuantity <= 0) {
                        continue;
                    }

                    Inventory inventory = inventoryMap.get(new SkuId(skuMapping.getSkuId()));
                    ReservedItem reservedItem = reserveInventory(
                        inventory,
                        requiredQuantity,
                        command.getOrderId(),
                        command.getTtlSeconds() != null ? command.getTtlSeconds() : DEFAULT_TTL_SECONDS
                    );
                    reservedItems.add(reservedItem);
                    modifiedInventories.add(inventory);
                }
            }
            
            // 6. 변경된 재고 정보 일괄 저장
            saveInventoryPort.saveAll(modifiedInventories);
            
            // 7. 성공 응답 생성
            return createSuccessResponse(sagaId, command.getOrderId(), reservedItems);
            
        } catch (InsufficientStockException | InvalidInventoryException e) {
            log.error("번들 재고 예약 실패: sagaId={}, error={}", sagaId, e.getMessage(), e);
            
            // 실패 응답 생성 (트랜잭션이 자동으로 롤백됨)
            return createFailureResponse(sagaId, command.getOrderId(), e.getMessage());
        }
    }
    
    private ReservedItem reserveInventory(
        Inventory inventory,
        int quantity,
        String orderId,
        int ttlSeconds
    ) {
        Quantity requestedQuantity = Quantity.of(quantity);
        
        // 재고 가용성 확인
        if (!inventory.canReserve(requestedQuantity)) {
            throw new InsufficientStockException(
                String.format("재고가 부족합니다. SKU: %s, 가용 재고: %d, 요청 수량: %d",
                    inventory.getSkuId().value(), 
                    inventory.getAvailableQuantity().value(), 
                    quantity)
            );
        }
        
        // 재고 예약
        ReservationId reservationId = inventory.reserve(requestedQuantity);
        
        // 예약 엔티티 생성
        LocalDateTime now = LocalDateTime.now(clock);
        Reservation reservation = Reservation.create(
            reservationId,
            inventory.getSkuId(),
            requestedQuantity,
            orderId,
            now.plusSeconds(ttlSeconds),
            now
        );
        
        // 예약 저장
        Reservation savedReservation = reservationRepository.save(reservation);
        
        return new ReservedItem(
            inventory.getSkuId(),
            savedReservation.getId(),
            savedReservation.getQuantity(),
            savedReservation.getExpiresAt()
        );
    }
    
    private BundleReservationResponse createSuccessResponse(
        String sagaId,
        String orderId,
        List<ReservedItem> reservedItems
    ) {
        List<BundleReservationResponse.SkuReservation> skuReservations = reservedItems.stream()
            .map(item -> BundleReservationResponse.SkuReservation.builder()
                .skuId(item.skuId.value())
                .reservationId(item.reservationId.value())
                .quantity(item.quantity.value())
                .expiresAt(item.expiresAt)
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
    
    // 내부 레코드: 예약된 항목 정보
    private record ReservedItem(
        SkuId skuId,
        ReservationId reservationId,
        Quantity quantity,
        LocalDateTime expiresAt
    ) {}
}