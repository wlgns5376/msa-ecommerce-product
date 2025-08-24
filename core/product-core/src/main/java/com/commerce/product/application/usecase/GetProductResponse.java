package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class GetProductResponse {
    String productId;
    String name;
    String description;
    ProductType type;
    ProductStatus status;
    List<ProductOptionResponse> options;
    
    @Value
    @Builder
    public static class ProductOptionResponse {
        String optionId;
        String name;
        BigDecimal price;
        String currency;
        List<SkuMappingResponse> skuMappings;
        boolean isAvailable;
        int availableQuantity;
    }
    
    @Value
    @Builder
    public static class SkuMappingResponse {
        String skuId;
        int quantity;
    }
}