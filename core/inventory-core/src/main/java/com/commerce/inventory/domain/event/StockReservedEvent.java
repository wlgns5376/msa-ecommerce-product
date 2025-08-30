package com.commerce.inventory.domain.event;

import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.SkuId;
import lombok.Getter;

@Getter
public class StockReservedEvent extends AbstractInventoryEvent {
    private final SkuId skuId;
    private final Reservation reservation;

    public StockReservedEvent(SkuId skuId, Reservation reservation) {
        super();
        this.skuId = skuId;
        this.reservation = reservation;
    }

    @Override
    public String getAggregateId() {
        return skuId.value();
    }

    @Override
    public String getEventType() {
        return "inventory.stock.reserved";
    }
}