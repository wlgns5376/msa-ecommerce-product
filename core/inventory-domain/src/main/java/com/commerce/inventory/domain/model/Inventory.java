package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.product.domain.model.AggregateRoot;
import lombok.Getter;

@Getter
public class Inventory extends AggregateRoot<SkuId> {
    
    private final SkuId skuId;
    private Quantity totalQuantity;
    private Quantity reservedQuantity;
    
    private Inventory(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity) {
        validateCreate(skuId, totalQuantity, reservedQuantity);
        
        this.skuId = skuId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
    }
    
    public static Inventory create(SkuId skuId, Quantity totalQuantity, Quantity reservedQuantity) {
        return new Inventory(skuId, totalQuantity, reservedQuantity);
    }
    
    public static Inventory createWithInitialStock(SkuId skuId, Quantity initialQuantity) {
        return new Inventory(skuId, initialQuantity, new Quantity(0));
    }
    
    public static Inventory createEmpty(SkuId skuId) {
        return new Inventory(skuId, new Quantity(0), new Quantity(0));
    }
    
    public Quantity getAvailableQuantity() {
        return totalQuantity.subtract(reservedQuantity);
    }
    
    public boolean canReserve(Quantity quantity) {
        return getAvailableQuantity().isGreaterThanOrEqual(quantity);
    }
    
    public void receive(Quantity quantity, String reference) {
        if (quantity == null || quantity.getValue() == 0) {
            throw new InvalidInventoryException("입고 수량은 0보다 커야 합니다");
        }
        
        this.totalQuantity = this.totalQuantity.add(quantity);
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new StockReceivedEvent(this.skuId, quantity, reference));
    }
    
    public ReservationId reserve(Quantity quantity, String orderId, int ttlSeconds) {
        if (!canReserve(quantity)) {
            throw new InsufficientStockException(
                String.format("재고가 부족합니다. 가용 재고: %d, 요청 수량: %d", 
                    getAvailableQuantity().getValue(), quantity.getValue())
            );
        }
        
        this.reservedQuantity = this.reservedQuantity.add(quantity);
        ReservationId reservationId = ReservationId.generate();
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new StockReservedEvent(this.skuId, reservationId, quantity, orderId));
        
        return reservationId;
    }
    
    public void releaseReservation(ReservationId reservationId, Quantity quantity) {
        this.reservedQuantity = this.reservedQuantity.subtract(quantity);
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new ReservationReleasedEvent(this.skuId, reservationId));
    }
    
    public void confirmReservation(ReservationId reservationId, Quantity quantity) {
        this.totalQuantity = this.totalQuantity.subtract(quantity);
        this.reservedQuantity = this.reservedQuantity.subtract(quantity);
        
        // 도메인 이벤트 발생 (추후 구현)
        // this.raise(new ReservationConfirmedEvent(this.skuId, reservationId));
    }
    
    public void deduct(Quantity quantity, String reference) {
        if (totalQuantity.getValue() < quantity.getValue()) {
            throw new InsufficientStockException(
                String.format("재고가 부족합니다. 총 재고: %d, 차감 요청: %d", 
                    totalQuantity.getValue(), quantity.getValue())
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
        
        if (reservedQuantity.getValue() > totalQuantity.getValue()) {
            throw new InvalidInventoryException("예약 수량은 총 수량을 초과할 수 없습니다");
        }
    }
    
    @Override
    public SkuId getId() {
        return skuId;
    }
}