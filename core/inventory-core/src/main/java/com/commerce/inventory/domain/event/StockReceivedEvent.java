package com.commerce.inventory.domain.event;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.SkuId;
import lombok.Getter;

@Getter
public class StockReceivedEvent extends AbstractInventoryEvent {
    private final SkuId skuId;
    private final Quantity quantity;
    private final String reference;

    public StockReceivedEvent(SkuId skuId, Quantity quantity, String reference) {
        super();
        this.skuId = skuId;
        this.quantity = quantity;
        this.reference = reference;
    }

    @Override
    public String getAggregateId() {
        return skuId.value();
    }

    @Override
    public String getEventType() {
        return "inventory.stock.received";
    }
}