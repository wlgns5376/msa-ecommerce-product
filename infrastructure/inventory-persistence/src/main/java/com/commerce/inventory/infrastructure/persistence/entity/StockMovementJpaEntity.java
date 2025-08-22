package com.commerce.inventory.infrastructure.persistence.entity;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.MovementId;
import com.commerce.inventory.domain.model.MovementType;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.model.StockMovement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementJpaEntity {
    
    @Id
    @Column(name = "movement_id", nullable = false, length = 36)
    private String movementId;
    
    @Column(name = "sku_id", nullable = false, length = 36)
    private String skuId;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;
    
    @Column(name = "reference", nullable = false)
    private String reference;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    public static StockMovementJpaEntity fromDomainModel(StockMovement stockMovement) {
        return StockMovementJpaEntity.builder()
                .movementId(stockMovement.getId().value())
                .skuId(stockMovement.getSkuId().value())
                .quantity(stockMovement.getQuantity().value())
                .movementType(stockMovement.getType())
                .reference(stockMovement.getReference())
                .timestamp(stockMovement.getTimestamp())
                .build();
    }
    
    public StockMovement toDomainModel() {
        return StockMovement.restore(
                new MovementId(movementId),
                SkuId.of(skuId),
                movementType,
                Quantity.of(quantity),
                reference,
                timestamp
        );
    }
}
