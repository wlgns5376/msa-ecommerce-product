package com.commerce.inventory.application.port.in;

public class InventoryResponse {
    
    private final String skuId;
    private final int totalQuantity;
    private final int reservedQuantity;
    private final int availableQuantity;
    
    public InventoryResponse(String skuId, int totalQuantity, int reservedQuantity, int availableQuantity) {
        this.skuId = skuId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
    }
    
    public String getSkuId() {
        return skuId;
    }
    
    public int getTotalQuantity() {
        return totalQuantity;
    }
    
    public int getReservedQuantity() {
        return reservedQuantity;
    }
    
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}