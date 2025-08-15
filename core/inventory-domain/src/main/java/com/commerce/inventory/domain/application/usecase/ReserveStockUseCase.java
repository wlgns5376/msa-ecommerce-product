package com.commerce.inventory.domain.application.usecase;

import com.commerce.common.application.usecase.UseCase;
import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidReservationException;
import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.repository.InventoryRepository;
import com.commerce.inventory.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ReserveStockUseCase implements UseCase<ReserveStockRequest, ReserveStockResponse> {
    
    private static final int DEFAULT_TTL_SECONDS = 900; // 15분
    
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    
    // 예약과 SKU ID를 함께 추적하기 위한 내부 클래스
    private static class ReservationWithSkuId {
        final Reservation reservation;
        final String skuId;
        
        ReservationWithSkuId(Reservation reservation, String skuId) {
            this.reservation = reservation;
            this.skuId = skuId;
        }
    }
    
    @Override
    @Transactional
    public ReserveStockResponse execute(ReserveStockRequest request) {
        validateRequest(request);
        
        LocalDateTime currentTime = LocalDateTime.now(clock);
        int ttlSeconds = request.getTtlSeconds() != null ? request.getTtlSeconds() : DEFAULT_TTL_SECONDS;
        
        // 재고 확인 및 잠금
        Map<String, Inventory> inventoryMap = lockAndVerifyInventories(request.getItems());
        
        // 예약 도메인 객체 생성
        List<ReservationWithSkuId> reservationsWithSkuId = performReservations(
                request, inventoryMap, ttlSeconds, currentTime
        );
        
        // 예약 저장 (향후 saveAll 구현 시 성능 개선 가능)
        List<ReservationWithSkuId> savedReservationsWithSkuId = reservationsWithSkuId.stream()
                .map(rwsi -> new ReservationWithSkuId(
                        reservationRepository.save(rwsi.reservation), 
                        rwsi.skuId
                ))
                .collect(Collectors.toList());
        
        // 변경된 모든 재고를 한 번에 저장
        inventoryMap.values().forEach(inventoryRepository::save);
        
        // DTO 변환
        List<ReserveStockResponse.ReservationResult> results = convertToResults(savedReservationsWithSkuId);
        
        return ReserveStockResponse.builder()
                .reservations(results)
                .build();
    }
    
    private Map<String, Inventory> lockAndVerifyInventories(List<ReserveStockRequest.ReservationItem> items) {
        // SKU별로 요청 수량을 합산
        Map<String, Integer> totalQuantityBySku = items.stream()
                .collect(Collectors.groupingBy(
                        ReserveStockRequest.ReservationItem::getSkuId,
                        Collectors.summingInt(ReserveStockRequest.ReservationItem::getQuantity)
                ));
        
        // 모든 SKU ID를 수집하여 한 번의 쿼리로 조회
        Set<SkuId> skuIds = totalQuantityBySku.keySet().stream()
                .map(SkuId::new)
                .collect(Collectors.toSet());
        
        Map<SkuId, Inventory> inventoryMapBySkuId = inventoryRepository.findBySkuIdsWithLock(skuIds);
        
        // 합산된 수량으로 재고 검증
        Map<String, Inventory> inventoryMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : totalQuantityBySku.entrySet()) {
            String skuIdStr = entry.getKey();
            Integer totalQuantity = entry.getValue();
            SkuId skuId = new SkuId(skuIdStr);
            Inventory inventory = inventoryMapBySkuId.get(skuId);
            
            if (inventory == null) {
                throw new InvalidSkuIdException("SKU를 찾을 수 없습니다: " + skuIdStr);
            }

            if (!inventory.canReserve(Quantity.of(totalQuantity))) {
                throw new InsufficientStockException(
                        String.format("재고가 부족합니다. SKU: %s, 가용 재고: %d, 요청 수량: %d",
                                skuIdStr, inventory.getAvailableQuantity().value(), totalQuantity)
                );
            }
            inventoryMap.put(skuIdStr, inventory);
        }
        
        return inventoryMap;
    }
    
    private List<ReservationWithSkuId> performReservations(
            ReserveStockRequest request,
            Map<String, Inventory> inventoryMap,
            int ttlSeconds,
            LocalDateTime currentTime
    ) {
        return request.getItems().stream()
                .map(item -> {
                    Reservation reservation = createReservation(
                            item,
                            inventoryMap.get(item.getSkuId()),
                            request.getOrderId(),
                            ttlSeconds,
                            currentTime
                    );
                    return new ReservationWithSkuId(reservation, item.getSkuId());
                })
                .collect(Collectors.toList());
    }
    
    private Reservation createReservation(
            ReserveStockRequest.ReservationItem item,
            Inventory inventory,
            String orderId,
            int ttlSeconds,
            LocalDateTime currentTime
    ) {
        Quantity requestedQuantity = Quantity.of(item.getQuantity());
        
        ReservationId reservationId = inventory.reserve(requestedQuantity);
        
        return Reservation.create(
                reservationId,
                inventory.getId(),
                requestedQuantity,
                orderId,
                currentTime.plusSeconds(ttlSeconds),
                currentTime
        );
    }
    
    private List<ReserveStockResponse.ReservationResult> convertToResults(
            List<ReservationWithSkuId> savedReservationsWithSkuId
    ) {
        return savedReservationsWithSkuId.stream()
                .map(rwsi -> ReserveStockResponse.ReservationResult.builder()
                        .reservationId(rwsi.reservation.getId().value())
                        .skuId(rwsi.skuId)
                        .quantity(rwsi.reservation.getQuantity().value())
                        .expiresAt(rwsi.reservation.getExpiresAt())
                        .status(rwsi.reservation.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }
    
    private void validateRequest(ReserveStockRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidReservationException("예약 항목이 비어있습니다");
        }
        
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new InvalidReservationException("주문 ID는 필수입니다");
        }
        
        for (ReserveStockRequest.ReservationItem item : request.getItems()) {
            if (item.getSkuId() == null || item.getSkuId().trim().isEmpty()) {
                throw new InvalidReservationException("SKU ID는 필수입니다");
            }
            
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new InvalidReservationException("수량은 0보다 커야 합니다");
            }
        }
    }
}