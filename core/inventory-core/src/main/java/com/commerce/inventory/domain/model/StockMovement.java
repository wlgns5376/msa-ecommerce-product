package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.exception.InvalidStockMovementException;
import com.commerce.common.domain.model.AggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class StockMovement extends AggregateRoot<MovementId> {
    
    private final MovementId id;
    private final SkuId skuId;
    private final MovementType type;
    private final Quantity quantity;
    private final String reference;
    private final LocalDateTime timestamp;
    
    private StockMovement(
            MovementId id,
            SkuId skuId,
            MovementType type,
            Quantity quantity,
            String reference,
            LocalDateTime timestamp
    ) {
        validateCreate(id, skuId, type, quantity, reference);
        
        this.id = id;
        this.skuId = skuId;
        this.type = type;
        this.quantity = quantity;
        this.reference = reference;
        this.timestamp = timestamp;
    }
    
    public static StockMovement create(
            MovementId id,
            SkuId skuId,
            MovementType type,
            Quantity quantity,
            String reference,
            LocalDateTime timestamp
    ) {
        return new StockMovement(id, skuId, type, quantity, reference, timestamp);
    }
    
    public static StockMovement restore(
            MovementId id,
            SkuId skuId,
            MovementType type,
            Quantity quantity,
            String reference,
            LocalDateTime timestamp
    ) {
        return new StockMovement(id, skuId, type, quantity, reference, timestamp);
    }
    
    public static StockMovement create(
            SkuId skuId,
            Quantity quantity,
            MovementType type,
            String reference,
            LocalDateTime timestamp
    ) {
        return new StockMovement(MovementId.generate(), skuId, type, quantity, reference, timestamp);
    }
    
    public static StockMovement createInbound(SkuId skuId, Quantity quantity, String reference, LocalDateTime currentTime) {
        return new StockMovement(
                MovementId.generate(),
                skuId,
                MovementType.INBOUND,
                quantity,
                reference,
                currentTime
        );
    }
    
    public static StockMovement createOutbound(SkuId skuId, Quantity quantity, String reference, LocalDateTime currentTime) {
        return new StockMovement(
                MovementId.generate(),
                skuId,
                MovementType.OUTBOUND,
                quantity,
                reference,
                currentTime
        );
    }
    
    public static StockMovement createAdjustment(SkuId skuId, Quantity quantity, String reference, LocalDateTime currentTime) {
        return new StockMovement(
                MovementId.generate(),
                skuId,
                MovementType.ADJUSTMENT,
                quantity,
                reference,
                currentTime
        );
    }
    
    public boolean isInbound() {
        return type == MovementType.INBOUND;
    }
    
    public boolean isOutbound() {
        return type == MovementType.OUTBOUND;
    }
    
    private void validateCreate(
            MovementId id,
            SkuId skuId,
            MovementType type,
            Quantity quantity,
            String reference
    ) {
        if (id == null) {
            throw new InvalidStockMovementException("Movement ID는 필수입니다");
        }
        
        if (skuId == null) {
            throw new InvalidStockMovementException("SKU ID는 필수입니다");
        }
        
        if (type == null) {
            throw new InvalidStockMovementException("Movement 타입은 필수입니다");
        }
        
        if (quantity == null || quantity.value() == 0) {
            throw new InvalidStockMovementException("수량은 0보다 커야 합니다");
        }
        
        if (reference == null || reference.trim().isEmpty()) {
            throw new InvalidStockMovementException("참조 번호는 필수입니다");
        }
    }
    
    @Override
    public MovementId getId() {
        return id;
    }
}