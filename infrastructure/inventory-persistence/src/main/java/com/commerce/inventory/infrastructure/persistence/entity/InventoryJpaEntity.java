package com.commerce.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}