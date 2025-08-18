package com.commerce.inventory.application.port.in;

import com.commerce.inventory.domain.model.Inventory;

public record InventoryResponse(String skuId, int totalQuantity, int reservedQuantity, int availableQuantity) {
    
    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
            inventory.getSkuId().value(),
            inventory.getTotalQuantity().value(),
            inventory.getReservedQuantity().value(),
            inventory.getAvailableQuantity().value()
        );
    }
    
    public static InventoryResponse empty(String skuId) {
        return new InventoryResponse(skuId, 0, 0, 0);
    }
}