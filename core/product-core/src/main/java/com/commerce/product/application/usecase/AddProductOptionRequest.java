package com.commerce.product.application.usecase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class AddProductOptionRequest {
    @NotBlank(message = "상품 ID는 필수입니다.")
    private final String productId;
    
    @NotBlank(message = "옵션명은 필수입니다.")
    private final String optionName;
    
    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private final BigDecimal price;
    
    @NotBlank(message = "통화는 필수입니다.")
    private final String currency;
    
    @NotEmpty(message = "SKU 매핑은 필수입니다.")
    private final Map<String, Integer> skuMappings; // key: SKU ID, value: 해당 SKU 수량
}