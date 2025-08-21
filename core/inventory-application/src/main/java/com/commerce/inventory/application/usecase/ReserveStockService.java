package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.ReserveStockCommand;
import com.commerce.inventory.application.port.in.ReserveStockResponse;
import com.commerce.inventory.application.port.in.ReserveStockUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.application.util.ValidationHelper;
import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidReservationException;
import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveStockService implements ReserveStockUseCase {
    
    private static final int DEFAULT_TTL_SECONDS = 900; // 15분
    private static final int BATCH_SIZE = 1000; // IN 절 제한을 위한 배치 크기
    
    private final LoadInventoryPort loadInventoryPort;
    private final SaveInventoryPort saveInventoryPort;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    
    @Override
    public ReserveStockResponse execute(ReserveStockCommand request) {
        validateRequest(request);
        
        LocalDateTime currentTime = LocalDateTime.now(clock);
        int ttlSeconds = Optional.ofNullable(request.getTtlSeconds()).orElse(DEFAULT_TTL_SECONDS);
        
        // 재고 확인 및 잠금
        Map<String, Inventory> inventoryMap = lockAndVerifyInventories(request.getItems());
        
        // 예약 도메인 객체 생성
        List<Reservation> reservations = performReservations(
                request, inventoryMap, ttlSeconds, currentTime
        );
        
        // 예약 저장 - 일괄 처리로 성능 개선
        List<Reservation> savedReservations = reservationRepository.saveAll(reservations);
        
        // 변경된 모든 재고를 일괄 저장
        saveInventoryPort.saveAll(new ArrayList<>(inventoryMap.values()));
        
        // DTO 변환
        List<ReserveStockResponse.ReservationResult> results = convertToResults(savedReservations);
        
        return ReserveStockResponse.builder()
                .reservations(results)
                .build();
    }
    
    private Map<String, Inventory> lockAndVerifyInventories(List<ReserveStockCommand.ReservationItem> items) {
        // SKU별로 요청 수량을 합산
        Map<String, Integer> totalQuantityBySku = items.stream()
                .collect(Collectors.groupingBy(
                        ReserveStockCommand.ReservationItem::getSkuId,
                        Collectors.summingInt(ReserveStockCommand.ReservationItem::getQuantity)
                ));
        
        // 모든 SKU ID를 수집
        Set<SkuId> skuIds = totalQuantityBySku.keySet().stream()
                .map(SkuId::new)
                .collect(Collectors.toSet());
        
        // 대량의 SKU를 배치로 나누어 조회 (간소화된 로직)
        Map<SkuId, Inventory> inventoryMapBySkuId = fetchInventoriesInBatches(skuIds);
        
        // 존재하지 않는 SKU를 일괄 검증
        validateAllSkusExist(skuIds, inventoryMapBySkuId.keySet());
        
        // 합산된 수량으로 재고 검증 및 결과 맵 생성
        return validateAndBuildInventoryMap(totalQuantityBySku, inventoryMapBySkuId);
    }
    
    private Map<SkuId, Inventory> fetchInventoriesInBatches(Set<SkuId> skuIds) {
        Map<SkuId, Inventory> result = new HashMap<>();
        List<SkuId> skuIdList = new ArrayList<>(skuIds);
        
        for (int i = 0; i < skuIdList.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, skuIdList.size());
            Set<SkuId> batchSkuIds = new HashSet<>(skuIdList.subList(i, endIndex));
            result.putAll(loadInventoryPort.loadBySkuIdsWithLock(batchSkuIds));
        }
        
        return result;
    }
    
    private void validateAllSkusExist(Set<SkuId> requestedSkuIds, Set<SkuId> foundSkuIds) {
        if (foundSkuIds.size() == requestedSkuIds.size()) {
            return;
        }
        
        Set<SkuId> missingSkuIds = new HashSet<>(requestedSkuIds);
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
    
    private Map<String, Inventory> validateAndBuildInventoryMap(
            Map<String, Integer> totalQuantityBySku,
            Map<SkuId, Inventory> inventoryMapBySkuId) {
        
        Map<String, Inventory> result = new HashMap<>();
        
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
            result.put(skuIdStr, inventory);
        }
        
        return result;
    }
    
    private List<Reservation> performReservations(
            ReserveStockCommand request,
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
            ReserveStockCommand.ReservationItem item,
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
    
    private void validateRequest(ReserveStockCommand request) {
        ValidationHelper.validateNotNull(request, "예약 요청이 null일 수 없습니다");
        ValidationHelper.validateNotEmpty(request.getOrderId(), "주문 ID");
        ValidationHelper.validateNotEmptyList(request.getItems(), "예약 항목");
        
        for (ReserveStockCommand.ReservationItem item : request.getItems()) {
            ValidationHelper.validateNotNull(item, "예약 항목에 null이 포함될 수 없습니다");
            ValidationHelper.validateNotEmpty(item.getSkuId(), "SKU ID");
            ValidationHelper.validatePositive(item.getQuantity(), "수량");
        }
    }
}