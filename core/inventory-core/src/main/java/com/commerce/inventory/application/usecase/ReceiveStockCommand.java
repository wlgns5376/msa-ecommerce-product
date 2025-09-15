package com.commerce.inventory.application.usecase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceiveStockCommand {
    @NotBlank(message = "SKU ID는 필수입니다")
    private final String skuId;
    
    @Positive(message = "입고 수량은 0보다 커야 합니다")
    private final int quantity;
    
    @NotBlank(message = "참조 번호는 필수입니다")
    private final String reference;
}