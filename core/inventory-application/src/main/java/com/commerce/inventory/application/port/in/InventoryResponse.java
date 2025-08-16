package com.commerce.inventory.application.port.in;

public record InventoryResponse(String skuId, int totalQuantity, int reservedQuantity, int availableQuantity) {
}