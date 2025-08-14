package com.commerce.inventory.domain.exception;

import com.commerce.inventory.domain.model.SkuId;

public class InvalidSkuException extends InventoryDomainException {
    
    public InvalidSkuException(String message) {
        super(message);
    }
    
    public static InvalidSkuException notFound(SkuId skuId) {
        return new InvalidSkuException("존재하지 않는 SKU입니다: " + skuId.value());
    }
    
    @Override
    public String getErrorCode() {
        return "INVENTORY_INVALID_SKU";
    }
}