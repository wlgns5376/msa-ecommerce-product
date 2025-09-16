package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.common.domain.model.Quantity;
import com.commerce.product.domain.model.inventory.Inventory;
import com.commerce.product.domain.model.inventory.SkuId;
import com.commerce.product.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Inventory 서비스와의 통신을 담당하는 Adapter
 * Product 서비스에서 Inventory 서비스의 재고 정보를 조회/수정할 때 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryRepositoryAdapter implements InventoryRepository {
    
    private final WebClient inventoryServiceWebClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    
    @Override
    public int getAvailableQuantity(String skuId) {
        try {
            return inventoryServiceWebClient
                    .get()
                    .uri("/api/inventory/{skuId}", skuId)
                    .retrieve()
                    .onStatus(
                            status -> status.equals(HttpStatus.NOT_FOUND),
                            response -> Mono.empty()
                    )
                    .bodyToMono(InventoryResponse.class)
                    .map(InventoryResponse::getAvailableQuantity)
                    .timeout(TIMEOUT)
                    .doOnError(error -> log.error("Failed to get available quantity for SKU: {}", skuId, error))
                    .onErrorReturn(0)
                    .block();
        } catch (Exception e) {
            log.error("Error getting available quantity for SKU: {}", skuId, e);
            return 0;
        }
    }
    
    @Override
    public String reserveStock(String skuId, int quantity, String orderId) {
        try {
            ReservationRequest request = ReservationRequest.builder()
                    .skuId(skuId)
                    .quantity(quantity)
                    .orderId(orderId)
                    .ttl(300) // 5 minutes default TTL
                    .build();
                    
            return inventoryServiceWebClient
                    .post()
                    .uri("/api/inventory/reservations")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ReservationResponse.class)
                    .map(ReservationResponse::getReservationId)
                    .timeout(TIMEOUT)
                    .doOnError(error -> log.error("Failed to reserve stock for SKU: {}, quantity: {}", skuId, quantity, error))
                    .onErrorResume(error -> Mono.error(new RuntimeException("재고 예약에 실패했습니다.")))
                    .block();
        } catch (Exception e) {
            log.error("Error reserving stock for SKU: {}, quantity: {}", skuId, quantity, e);
            throw new RuntimeException("재고 예약 중 오류가 발생했습니다.", e);
        }
    }
    
    @Override
    public void releaseReservation(String reservationId) {
        try {
            inventoryServiceWebClient
                    .delete()
                    .uri("/api/inventory/reservations/{id}", reservationId)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(TIMEOUT)
                    .doOnError(error -> log.error("Failed to release reservation: {}", reservationId, error))
                    .block();
        } catch (Exception e) {
            log.error("Error releasing reservation: {}", reservationId, e);
            // 예약 해제 실패는 로깅만 하고 예외를 전파하지 않음
            // 백그라운드 프로세스가 만료된 예약을 처리할 것임
        }
    }
    
    @Override
    public Optional<Inventory> findBySkuId(SkuId skuId) {
        try {
            return inventoryServiceWebClient
                    .get()
                    .uri("/api/inventory/{skuId}", skuId.value())
                    .retrieve()
                    .onStatus(
                            status -> status.equals(HttpStatus.NOT_FOUND),
                            response -> Mono.empty()
                    )
                    .bodyToMono(InventoryResponse.class)
                    .map(response -> new InventoryImpl(
                            new SkuId(response.getSkuId()),
                            response.getTotalQuantity(),
                            response.getReservedQuantity(),
                            response.getAvailableQuantity()
                    ))
                    .map(inv -> (Inventory) inv)
                    .timeout(TIMEOUT)
                    .blockOptional();
        } catch (Exception e) {
            log.error("Error finding inventory for SKU: {}", skuId.value(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public void save(Inventory inventory) {
        // Product 서비스는 Inventory를 직접 저장하지 않음
        // 이 메서드는 테스트 목적으로만 사용될 수 있음
        throw new UnsupportedOperationException("Product service cannot directly save inventory");
    }
    
    @Override
    public Map<SkuId, Inventory> findBySkuIds(List<SkuId> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }
        
        try {
            List<String> skuIdStrings = skuIds.stream()
                    .map(SkuId::value)
                    .distinct()
                    .collect(Collectors.toList());
            
            return Flux.fromIterable(skuIdStrings)
                    .flatMap(skuId -> inventoryServiceWebClient
                            .get()
                            .uri("/api/inventory/{skuId}", skuId)
                            .retrieve()
                            .onStatus(
                                    status -> status.equals(HttpStatus.NOT_FOUND),
                                    response -> Mono.empty()
                            )
                            .bodyToMono(InventoryResponse.class)
                            .onErrorResume(error -> {
                                log.error("Error fetching inventory for SKU: {}", skuId, error);
                                return Mono.empty();
                            })
                    )
                    .collectMap(
                            response -> new SkuId(response.getSkuId()),
                            response -> (Inventory) new InventoryImpl(
                                    new SkuId(response.getSkuId()),
                                    response.getTotalQuantity(),
                                    response.getReservedQuantity(),
                                    response.getAvailableQuantity()
                            )
                    )
                    .timeout(TIMEOUT)
                    .block();
        } catch (Exception e) {
            log.error("Error finding inventory for multiple SKUs", e);
            return Map.of();
        }
    }
    
    /**
     * Inventory 서비스 응답 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    private static class InventoryResponse {
        private String skuId;
        private int totalQuantity;
        private int reservedQuantity;
        private int availableQuantity;
    }
    
    /**
     * 재고 예약 요청 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    private static class ReservationRequest {
        private String skuId;
        private int quantity;
        private String orderId;
        private int ttl;
    }
    
    /**
     * 재고 예약 응답 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    private static class ReservationResponse {
        private String reservationId;
        private String skuId;
        private int quantity;
        private String orderId;
        private String expiresAt;
    }
    
    /**
     * Inventory 인터페이스의 구현체
     */
    @lombok.RequiredArgsConstructor
    @lombok.Getter
    private static class InventoryImpl implements Inventory {
        private final SkuId skuId;
        private final int totalQuantity;
        private final int reservedQuantity;
        private final int availableQuantity;
        
        @Override
        public Quantity getAvailableQuantity() {
            return Quantity.of(availableQuantity);
        }
    }
    
    public boolean existsById(Object id) {
        // InventoryRepository는 Repository 인터페이스를 상속받지 않으므로
        // 이 메소드는 필요하지 않음
        throw new UnsupportedOperationException("existsById is not supported in InventoryRepository");
    }
    
    public long count() {
        // Product 도메인에서는 이 메소드를 사용하지 않음
        throw new UnsupportedOperationException("count is not supported in InventoryRepository");
    }
    
    public void deleteById(Object id) {
        // Product 도메인에서는 inventory를 직접 삭제하지 않음
        throw new UnsupportedOperationException("deleteById is not supported in InventoryRepository");
    }
    
    public List<?> findAll() {
        // Product 도메인에서는 이 메소드를 사용하지 않음
        throw new UnsupportedOperationException("findAll is not supported in InventoryRepository");
    }
    
    public void delete(Object entity) {
        // Product 도메인에서는 inventory를 직접 삭제하지 않음
        throw new UnsupportedOperationException("delete is not supported in InventoryRepository");
    }
    
    @Override
    public Optional<Inventory> findById(Object id) {
        if (id instanceof SkuId) {
            return findBySkuId((SkuId) id);
        }
        return Optional.empty();
    }
    
    @Override
    public Inventory save(Object entity) {
        if (entity instanceof Inventory) {
            save((Inventory) entity);
            return (Inventory) entity;
        }
        throw new IllegalArgumentException("Entity must be an Inventory instance");
    }
    
    @Override
    public List<Inventory> saveAll(List entities) {
        throw new UnsupportedOperationException("saveAll is not supported in InventoryRepository");
    }
}