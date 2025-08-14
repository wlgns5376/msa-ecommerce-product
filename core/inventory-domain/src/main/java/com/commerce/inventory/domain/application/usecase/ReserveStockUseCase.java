package com.commerce.inventory.domain.application.usecase;

import com.commerce.common.application.usecase.UseCase;
import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidReservationException;
import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.repository.InventoryRepository;
import com.commerce.inventory.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ReserveStockUseCase implements UseCase<ReserveStockRequest, ReserveStockResponse> {
    
    private static final int DEFAULT_TTL_SECONDS = 900; // 15분
    
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    
    @Override
    @Transactional
    public ReserveStockResponse execute(ReserveStockRequest request) {
        validateRequest(request);
        
        LocalDateTime currentTime = LocalDateTime.now(clock);
        int ttlSeconds = request.getTtlSeconds() != null ? request.getTtlSeconds() : DEFAULT_TTL_SECONDS;
        
        // 먼저 모든 재고를 검증하고 Map에 저장
        Map<String, Inventory> inventoryMap = new HashMap<>();
        for (ReserveStockRequest.ReservationItem item : request.getItems()) {
            SkuId skuId = new SkuId(item.getSkuId());
            Inventory inventory = inventoryRepository.findBySkuIdWithLock(skuId)
                    .orElseThrow(() -> new InvalidSkuIdException("SKU를 찾을 수 없습니다: " + item.getSkuId()));

            if (!inventory.canReserve(Quantity.of(item.getQuantity()))) {
                throw new InsufficientStockException(
                        String.format("재고가 부족합니다. SKU: %s, 가용 재고: %d, 요청 수량: %d",
                                item.getSkuId(), inventory.getAvailableQuantity().value(), item.getQuantity())
                );
            }
            inventoryMap.put(item.getSkuId(), inventory);
        }

        // 모든 재고가 충분하면 예약 진행
        List<ReserveStockResponse.ReservationResult> results = request.getItems().stream()
                .map(item -> reserveSingleItem(
                        item,
                        inventoryMap.get(item.getSkuId()),
                        request.getOrderId(),
                        ttlSeconds,
                        currentTime
                ))
                .collect(Collectors.toList());
        
        return ReserveStockResponse.builder()
                .reservations(results)
                .allSuccessful(true)
                .build();
    }
    
    private ReserveStockResponse.ReservationResult reserveSingleItem(
            ReserveStockRequest.ReservationItem item,
            Inventory inventory,
            String orderId,
            int ttlSeconds,
            LocalDateTime currentTime
    ) {
        SkuId skuId = new SkuId(item.getSkuId());
        Quantity requestedQuantity = Quantity.of(item.getQuantity());
        
        inventory.reserve(requestedQuantity, orderId, ttlSeconds);
        
        Reservation reservation = Reservation.createWithTTL(
                skuId,
                requestedQuantity,
                orderId,
                ttlSeconds,
                currentTime
        );
        
        Reservation savedReservation = reservationRepository.save(reservation);
        inventoryRepository.save(inventory);
        
        return ReserveStockResponse.ReservationResult.builder()
                .reservationId(savedReservation.getId().value())
                .skuId(item.getSkuId())
                .quantity(item.getQuantity())
                .expiresAt(savedReservation.getExpiresAt())
                .status(savedReservation.getStatus().name())
                .build();
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