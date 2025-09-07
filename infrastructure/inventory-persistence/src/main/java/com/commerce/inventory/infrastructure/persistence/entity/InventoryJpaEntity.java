package com.commerce.inventory.infrastructure.persistence.entity;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class InventoryJpaEntity {
    
    @Id
    @Column(name = "sku_id", nullable = false, length = 36)
    private String skuId;
    
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;
    
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public static InventoryJpaEntity fromDomainModel(Inventory inventory) {
        return InventoryJpaEntity.builder()
                .skuId(inventory.getSkuId().value())
                .totalQuantity(inventory.getTotalQuantity().value())
                .reservedQuantity(inventory.getReservedQuantity().value())
                .version(inventory.getVersion())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
    
    public Inventory toDomainModel() {
        return Inventory.restore(
                SkuId.of(skuId),
                Quantity.of(totalQuantity),
                Quantity.of(reservedQuantity),
                version,
                createdAt,
                updatedAt
        );
    }
}