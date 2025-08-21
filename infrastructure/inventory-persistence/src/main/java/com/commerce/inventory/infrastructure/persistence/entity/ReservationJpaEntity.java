package com.commerce.inventory.infrastructure.persistence.entity;

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
}