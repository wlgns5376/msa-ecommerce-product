package com.commerce.product.api.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProductOptionRequest {
    
    @NotBlank(message = "옵션명은 필수입니다.")
    private String optionName;
    
    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private BigDecimal price;
    
    @NotBlank(message = "통화는 필수입니다.")
    private String currency;
    
    @NotEmpty(message = "SKU 매핑은 필수입니다.")
    private Map<String, Integer> skuMappings; // key: SKU ID, value: 해당 SKU 수량
    
    public com.commerce.product.application.usecase.AddProductOptionRequest toUseCaseRequest(String productId) {
        return com.commerce.product.application.usecase.AddProductOptionRequest.builder()
                .productId(productId)
                .optionName(optionName)
                .price(price)
                .currency(currency)
                .skuMappings(skuMappings) // SKU ID -> 수량 매핑
                .build();
    }
}