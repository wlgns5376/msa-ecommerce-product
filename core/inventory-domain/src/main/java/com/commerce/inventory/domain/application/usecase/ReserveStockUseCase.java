package com.commerce.inventory.domain.application.usecase;

import com.commerce.common.application.usecase.UseCase;
import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.application.usecase.validation.RequestValidator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ReserveStockUseCase implements UseCase<ReserveStockRequest, ReserveStockResponse> {
    
    private static final int DEFAULT_TTL_SECONDS = 900; // 15분
    private static final int BATCH_SIZE = 1000; // IN 절 제한을 위한 배치 크기
    
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    
    
    @Override
    @Transactional
    public ReserveStockResponse execute(ReserveStockRequest request) {
        validateRequest(request);
        
        LocalDateTime currentTime = LocalDateTime.now(clock);
        int ttlSeconds = request.getTtlSeconds() != null ? request.getTtlSeconds() : DEFAULT_TTL_SECONDS;
        
        // 재고 확인 및 잠금
        Map<String, Inventory> inventoryMap = lockAndVerifyInventories(request.getItems());
        
        // 예약 도메인 객체 생성
        List<Reservation> reservations = performReservations(
                request, inventoryMap, ttlSeconds, currentTime
        );
        
        // 예약 저장 - 일괄 처리로 성능 개선
        List<Reservation> savedReservations = reservationRepository.saveAll(reservations);
        
        // 변경된 모든 재고를 일괄 저장
        inventoryRepository.saveAll(new ArrayList<>(inventoryMap.values()));
        
        // DTO 변환
        List<ReserveStockResponse.ReservationResult> results = convertToResults(savedReservations);
        
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
        
        // 모든 SKU ID를 수집
        Set<SkuId> skuIds = totalQuantityBySku.keySet().stream()
                .map(SkuId::new)
                .collect(Collectors.toSet());
        
        // 대량의 SKU를 배치로 나누어 조회
        Map<SkuId, Inventory> inventoryMapBySkuId = new HashMap<>();
        List<SkuId> skuIdList = new ArrayList<>(skuIds);
        for (int i = 0; i < skuIdList.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, skuIdList.size());
            Set<SkuId> batchSkuIds = new HashSet<>(skuIdList.subList(i, endIndex));
            Map<SkuId, Inventory> batchResult = inventoryRepository.findBySkuIdsWithLock(batchSkuIds);
            inventoryMapBySkuId.putAll(batchResult);
        }
        
        // 존재하지 않는 SKU를 일괄 검증
        Set<SkuId> foundSkuIds = inventoryMapBySkuId.keySet();
        if (foundSkuIds.size() != skuIds.size()) {
            Set<SkuId> missingSkuIds = new HashSet<>(skuIds);
            missingSkuIds.removeAll(foundSkuIds);
            String missingIdsStr = missingSkuIds.stream()
                    .map(SkuId::value)
                    .limit(10)
                    .collect(Collectors.joining(", "));
            if (missingSkuIds.size() > 10) {
                missingIdsStr += " 외 " + (missingSkuIds.size() - 10) + "개";
            }
            throw new InvalidSkuIdException("다음 SKU를 찾을 수 없습니다: " + missingIdsStr);
        }
        
        // 합산된 수량으로 재고 검증
        Map<String, Inventory> inventoryMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : totalQuantityBySku.entrySet()) {
            String skuIdStr = entry.getKey();
            Integer totalQuantity = entry.getValue();
            SkuId skuId = new SkuId(skuIdStr);
            Inventory inventory = inventoryMapBySkuId.get(skuId);

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
    
    private List<Reservation> performReservations(
            ReserveStockRequest request,
            Map<String, Inventory> inventoryMap,
            int ttlSeconds,
            LocalDateTime currentTime
    ) {
        return request.getItems().stream()
                .map(item -> createReservation(
                        item,
                        inventoryMap.get(item.getSkuId()),
                        request.getOrderId(),
                        ttlSeconds,
                        currentTime
                ))
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
            List<Reservation> savedReservations
    ) {
        return savedReservations.stream()
                .map(reservation -> ReserveStockResponse.ReservationResult.builder()
                        .reservationId(reservation.getId().value())
                        .skuId(reservation.getSkuId().value())
                        .quantity(reservation.getQuantity().value())
                        .expiresAt(reservation.getExpiresAt())
                        .status(reservation.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }
    
    private void validateRequest(ReserveStockRequest request) {
        RequestValidator.of(request)
                .notEmptyList(ReserveStockRequest::getItems, "예약 항목")
                .notEmpty(ReserveStockRequest::getOrderId, "주문 ID")
                .validateEach(ReserveStockRequest::getItems, itemValidator -> itemValidator
                        .validate(item -> item != null, "예약 항목 중에 null 값이 포함될 수 없습니다.")
                        .notEmpty(ReserveStockRequest.ReservationItem::getSkuId, "SKU ID")
                        .positive(ReserveStockRequest.ReservationItem::getQuantity, "예약 수량")
                )
                .execute();
    }
}