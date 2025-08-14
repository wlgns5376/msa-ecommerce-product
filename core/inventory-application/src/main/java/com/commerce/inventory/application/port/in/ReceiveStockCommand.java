package com.commerce.inventory.application.port.in;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceiveStockCommand {
    private final String skuId;
    private final int quantity;
    private final String reference;
    
    public void validate() {
        if (skuId == null || skuId.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID는 필수입니다");
        }
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("입고 수량은 0보다 커야 합니다");
        }
        
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("참조 번호는 필수입니다");
        }
    }
}