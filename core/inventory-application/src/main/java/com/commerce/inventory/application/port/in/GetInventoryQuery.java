package com.commerce.inventory.application.port.in;

import jakarta.validation.constraints.NotNull;

public class GetInventoryQuery {
    
    @NotNull(message = "SKU ID is required")
    private final String skuId;
    
    public GetInventoryQuery(String skuId) {
        this.skuId = skuId;
    }
    
    public String getSkuId() {
        return skuId;
    }
}