package com.commerce.inventory.domain.event;

import com.commerce.inventory.domain.model.SkuId;
import lombok.Getter;

@Getter
public class StockDepletedEvent extends AbstractInventoryEvent {
    private final SkuId skuId;

    public StockDepletedEvent(SkuId skuId) {
        super();
        this.skuId = skuId;
    }

    @Override
    public String getAggregateId() {
        return skuId.value();
    }

    @Override
    public String getEventType() {
        return "inventory.stock.depleted";
    }
}