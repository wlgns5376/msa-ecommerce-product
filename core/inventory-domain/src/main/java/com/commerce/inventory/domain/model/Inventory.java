package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.common.domain.model.AggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Inventory extends AggregateRoot<SkuId> {
    
    private final SkuId skuId;
    private Quantity totalQuantity;
    private Quantity reservedQuantity;
    private Long version;
    
    private Inventory(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity) {
        this(skuId, totalQuantity, reservedQuantity, 0L);
    }
    
    private Inventory(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity, Long version) {
        super();
        validateCreate(skuId, totalQuantity, reservedQuantity);
        
        this.skuId = skuId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
        this.version = version;
    }
    
    private Inventory(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
        validateCreate(skuId, totalQuantity, reservedQuantity);
        
        this.skuId = skuId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
        this.version = version;
    }
    
    public static Inventory create(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity) {
        return new Inventory(skuId, totalQuantity, reservedQuantity);
    }
    
    public static Inventory createWithInitialStock(SkuId skuId, Quantity initialQuantity) {
        return new Inventory(skuId, initialQuantity, Quantity.zero());
    }
    
    public static Inventory createEmpty(SkuId skuId) {
        return new Inventory(skuId, Quantity.zero(), Quantity.zero());
    }
    
    public static Inventory restore(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity, Long version) {
        return new Inventory(skuId, totalQuantity, reservedQuantity, version);
    }
    
    public static Inventory restore(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Inventory(skuId, totalQuantity, reservedQuantity, version, createdAt, updatedAt);
    }
    
    public Quantity getAvailableQuantity() {
        return totalQuantity.subtract(reservedQuantity);
    }
    
    public boolean canReserve(Quantity quantity) {
        return getAvailableQuantity().isGreaterThanOrEqualTo(quantity);
    }
    
    public void receive(Quantity quantity) {
        if (quantity == null || quantity.value() <= 0) {
            throw new InvalidInventoryException("입고 수량은 0보다 커야 합니다");
        }
        
        this.totalQuantity = this.totalQuantity.add(quantity);
        updateTimestamp();
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new StockReceivedEvent(this.skuId, quantity));
    }
    
    public ReservationId reserve(Quantity quantity) {
        if (!canReserve(quantity)) {
            throw new InsufficientStockException(
                String.format("재고가 부족합니다. 가용 재고: %d, 요청 수량: %d", 
                    getAvailableQuantity().value(), quantity.value())
            );
        }
        
        this.reservedQuantity = this.reservedQuantity.add(quantity);
        ReservationId reservationId = ReservationId.generate();
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new StockReservedEvent(this.skuId, reservationId, quantity));
        
        return reservationId;
    }
    
    public void releaseReservedQuantity(Quantity quantity) {
        if (quantity == null || quantity.value() == 0) {
            throw new InvalidInventoryException("해제할 수량은 0보다 커야 합니다");
        }
        
        if (reservedQuantity.value() < quantity.value()) {
            throw new InvalidInventoryException(
                String.format("해제할 예약 수량이 부족합니다. 현재 예약: %d, 해제 요청: %d",
                    reservedQuantity.value(), quantity.value())
            );
        }
        
        this.reservedQuantity = this.reservedQuantity.subtract(quantity);
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new ReservationReleasedEvent(this.skuId, quantity));
    }
    
    public void confirmReservedQuantity(Quantity quantity) {
        if (quantity == null || quantity.value() == 0) {
            throw new InvalidInventoryException("확정할 수량은 0보다 커야 합니다");
        }
        
        if (reservedQuantity.value() < quantity.value()) {
            throw new InvalidInventoryException(
                String.format("확정할 예약 수량이 부족합니다. 현재 예약: %d, 확정 요청: %d",
                    reservedQuantity.value(), quantity.value())
            );
        }
        
        this.totalQuantity = this.totalQuantity.subtract(quantity);
        this.reservedQuantity = this.reservedQuantity.subtract(quantity);
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new ReservationConfirmedEvent(this.skuId, quantity));
    }
    
    public void deduct(Quantity quantity, String reference) {
        if (totalQuantity.value() < quantity.value()) {
            throw new InsufficientStockException(
                String.format("재고가 부족합니다. 총 재고: %d, 차감 요청: %d", 
                    totalQuantity.value(), quantity.value())
            );
        }
        
        this.totalQuantity = this.totalQuantity.subtract(quantity);
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new StockDeductedEvent(this.skuId, quantity, reference));
    }
    
    private void validateCreate(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity) {
        if (skuId == null) {
            throw new InvalidInventoryException("SKU ID는 필수입니다");
        }
        
        if (totalQuantity == null) {
            throw new InvalidInventoryException("총 수량은 필수입니다");
        }
        
        if (reservedQuantity == null) {
            throw new InvalidInventoryException("예약 수량은 필수입니다");
        }
        
        if (reservedQuantity.value() > totalQuantity.value()) {
            throw new InvalidInventoryException("예약 수량은 총 수량을 초과할 수 없습니다");
        }
    }
    
    @Override
    public SkuId getId() {
        return skuId;
    }
}