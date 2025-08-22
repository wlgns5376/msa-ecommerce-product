package com.commerce.product.domain.event;

import com.commerce.product.domain.model.SkuMapping;
import lombok.Getter;

@Getter
public class BundleReservationCompletedEvent extends AbstractDomainEvent {
    
    private final String reservationId;
    private final String orderId;
    private final SkuMapping skuMapping;
    
    public BundleReservationCompletedEvent(String reservationId, String orderId, SkuMapping skuMapping) {
        super();
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.skuMapping = skuMapping;
    }
    
    @Override
    public String getAggregateId() {
        return reservationId;
    }
    
    @Override
    public String getEventType() {
        return "bundle.reservation.completed";
    }
}