package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.BundleReservationResponse;
import com.commerce.inventory.application.port.in.ReserveBundleStockCommand;
import com.commerce.inventory.application.port.in.ReserveBundleStockUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.repository.ReservationRepository;
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
    
    @Override
    @Transactional
    public BundleReservationResponse execute(ReserveBundleStockCommand command) {
        validateCommand(command);
        
        String sagaId = command.getReservationId();
        List<ReservedItem> reservedItems = new ArrayList<>();
        Set<Inventory> modifiedInventories = new HashSet<>();
        
        try {
            // 모든 SKU ID를 수집하여 한 번에 조회
            List<SkuId> allSkuIds = command.getBundleItems().stream()
                .flatMap(bundleItem -> bundleItem.getSkuMappings().stream()
                    .map(mapping -> new SkuId(mapping.getSkuId())))
                .distinct()
                .collect(Collectors.toList());
            
            Map<SkuId, Inventory> inventoryMap = loadInventoryPort.loadAllByIds(allSkuIds);
            
            // 모든 번들 항목을 순차적으로 처리
            for (ReserveBundleStockCommand.BundleItem bundleItem : command.getBundleItems()) {
                List<ReservedItem> bundleReservedItems = reserveBundleItem(
                    bundleItem, 
                    command.getOrderId(), 
                    command.getTtlSeconds(),
                    inventoryMap,
                    modifiedInventories
                );
                reservedItems.addAll(bundleReservedItems);
            }
            
            // 변경된 재고 정보를 일괄 저장
            modifiedInventories.forEach(saveInventoryPort::save);
            
            // 성공 응답 생성
            return createSuccessResponse(sagaId, command.getOrderId(), reservedItems);
            
        } catch (InsufficientStockException | InvalidInventoryException | IllegalArgumentException e) {
            log.error("번들 재고 예약 실패: sagaId={}, error={}", sagaId, e.getMessage(), e);
            
            // 실패 응답 생성 (트랜잭션이 자동으로 롤백됨)
            return createFailureResponse(sagaId, command.getOrderId(), e.getMessage());
        }
    }
    
    private List<ReservedItem> reserveBundleItem(
        ReserveBundleStockCommand.BundleItem bundleItem,
        String orderId,
        Integer ttlSeconds,
        Map<SkuId, Inventory> inventoryMap,
        Set<Inventory> modifiedInventories
    ) {
        List<ReservedItem> reservedItems = new ArrayList<>();
        
        // 번들의 각 SKU에 대해 필요한 수량 계산
        for (ReserveBundleStockCommand.SkuMapping skuMapping : bundleItem.getSkuMappings()) {
            int requiredQuantity = skuMapping.getQuantity() * bundleItem.getQuantity();
            
            // 재고 조회 (이미 로드된 맵에서 가져옴)
            SkuId skuId = new SkuId(skuMapping.getSkuId());
            Inventory inventory = inventoryMap.get(skuId);
            if (inventory == null) {
                throw new InvalidInventoryException(
                    "재고를 찾을 수 없습니다: " + skuMapping.getSkuId()
                );
            }
            
            // 재고 예약
            ReservedItem reservedItem = reserveInventory(
                inventory,
                requiredQuantity,
                orderId,
                ttlSeconds != null ? ttlSeconds : DEFAULT_TTL_SECONDS
            );
            
            // 수정된 재고를 Set에 추가
            modifiedInventories.add(inventory);
            
            reservedItems.add(reservedItem);
        }
        
        return reservedItems;
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
                .status("ACTIVE")
                .build())
            .collect(Collectors.toList());
        
        return BundleReservationResponse.builder()
            .sagaId(sagaId)
            .orderId(orderId)
            .status("COMPLETED")
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
            .status("FAILED")
            .skuReservations(Collections.emptyList())
            .failureReason(failureReason)
            .build();
    }
    
    private void validateCommand(ReserveBundleStockCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("예약 요청이 null일 수 없습니다");
        }
        
        if (command.getOrderId() == null || command.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("주문 ID는 필수입니다");
        }
        
        if (command.getReservationId() == null || command.getReservationId().trim().isEmpty()) {
            throw new IllegalArgumentException("예약 ID는 필수입니다");
        }
        
        if (command.getBundleItems() == null || command.getBundleItems().isEmpty()) {
            throw new IllegalArgumentException("번들 항목은 최소 1개 이상이어야 합니다");
        }
        
        // 각 번들 항목 검증
        for (ReserveBundleStockCommand.BundleItem item : command.getBundleItems()) {
            validateBundleItem(item);
        }
    }
    
    private void validateBundleItem(ReserveBundleStockCommand.BundleItem item) {
        if (item == null) {
            throw new IllegalArgumentException("번들 항목이 null일 수 없습니다");
        }
        
        if (item.getProductOptionId() == null || item.getProductOptionId().trim().isEmpty()) {
            throw new IllegalArgumentException("상품 옵션 ID는 필수입니다");
        }
        
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("번들 수량은 0보다 커야 합니다");
        }
        
        if (item.getSkuMappings() == null || item.getSkuMappings().isEmpty()) {
            throw new IllegalArgumentException("SKU 매핑은 최소 1개 이상이어야 합니다");
        }
        
        // 각 SKU 매핑 검증
        for (ReserveBundleStockCommand.SkuMapping mapping : item.getSkuMappings()) {
            validateSkuMapping(mapping);
        }
    }
    
    private void validateSkuMapping(ReserveBundleStockCommand.SkuMapping mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("SKU 매핑이 null일 수 없습니다");
        }
        
        if (mapping.getSkuId() == null || mapping.getSkuId().trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID는 필수입니다");
        }
        
        if (mapping.getQuantity() == null || mapping.getQuantity() <= 0) {
            throw new IllegalArgumentException("SKU 수량은 0보다 커야 합니다");
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