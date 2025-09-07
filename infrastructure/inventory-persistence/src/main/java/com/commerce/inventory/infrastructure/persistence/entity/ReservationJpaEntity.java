package com.commerce.inventory.infrastructure.persistence.entity;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.ReservationStatus;
import com.commerce.inventory.domain.model.SkuId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_order_inventory", columnList = "order_id, inventory_id"),
    @Index(name = "idx_reservation_order", columnList = "order_id"),
    @Index(name = "idx_reservation_sku", columnList = "sku_id"),
    @Index(name = "idx_reservation_expires", columnList = "expires_at"),
    @Index(name = "idx_reservation_status", columnList = "status")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationJpaEntity {
    
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;
    
    @Column(name = "sku_id", length = 36, nullable = false)
    private String skuId;
    
    @Column(name = "inventory_id", length = 36, nullable = false)
    private String inventoryId;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "order_id", length = 100, nullable = false)
    private String orderId;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReservationStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    public enum ReservationStatus {
        ACTIVE,
        CONFIRMED,
        RELEASED,
        EXPIRED
    }
    
    public static ReservationJpaEntity fromDomainModel(Reservation reservation) {
        return ReservationJpaEntity.builder()
                .id(reservation.getId().value())
                .skuId(reservation.getSkuId().value())
                .inventoryId(reservation.getSkuId().value()) // SKU ID를 inventory ID로 사용
                .quantity(reservation.getQuantity().value())
                .orderId(reservation.getOrderId())
                .expiresAt(reservation.getExpiresAt())
                .status(mapToJpaStatus(reservation.getStatus()))
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .version(reservation.getVersion())
                .build();
    }
    
    public Reservation toDomainModel() {
        return Reservation.restore(
                new ReservationId(id),
                new SkuId(skuId),
                Quantity.of(quantity),
                orderId,
                expiresAt,
                mapToDomainStatus(status),
                createdAt,
                version
        );
    }
    
    private static ReservationStatus mapToJpaStatus(com.commerce.inventory.domain.model.ReservationStatus domainStatus) {
        return switch (domainStatus) {
            case ACTIVE -> ReservationStatus.ACTIVE;
            case CONFIRMED -> ReservationStatus.CONFIRMED;
            case RELEASED -> ReservationStatus.RELEASED;
            case EXPIRED -> ReservationStatus.EXPIRED;
        };
    }
    
    private static com.commerce.inventory.domain.model.ReservationStatus mapToDomainStatus(ReservationStatus jpaStatus) {
        return switch (jpaStatus) {
            case ACTIVE -> com.commerce.inventory.domain.model.ReservationStatus.ACTIVE;
            case CONFIRMED -> com.commerce.inventory.domain.model.ReservationStatus.CONFIRMED;
            case RELEASED -> com.commerce.inventory.domain.model.ReservationStatus.RELEASED;
            case EXPIRED -> com.commerce.inventory.domain.model.ReservationStatus.EXPIRED;
        };
    }
}