package com.commerce.inventory.domain.event;

import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.SkuId;
import lombok.Getter;

@Getter
public class ReservationReleasedEvent extends AbstractInventoryEvent {
    private final SkuId skuId;
    private final ReservationId reservationId;

    public ReservationReleasedEvent(SkuId skuId, ReservationId reservationId) {
        super();
        this.skuId = skuId;
        this.reservationId = reservationId;
    }

    @Override
    public String getAggregateId() {
        return skuId.value();
    }

    @Override
    public String getEventType() {
        return "inventory.reservation.released";
    }
}