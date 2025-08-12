package com.commerce.product.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BundleReservationFailedEvent extends AbstractDomainEvent {
    
    private final String reservationId;
    private final String orderId;
    private final String failureReason;
    
    public BundleReservationFailedEvent(String reservationId, String orderId, String failureReason) {
        super();
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.failureReason = failureReason;
    }
    
    @Override
    public String getAggregateId() {
        return reservationId;
    }
    
    @Override
    public String getEventType() {
        return "bundle.reservation.failed";
    }
}