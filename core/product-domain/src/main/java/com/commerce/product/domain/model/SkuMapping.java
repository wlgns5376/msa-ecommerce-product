package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidSkuMappingException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class SkuMapping implements ValueObject {
    private final String skuId;
    private final int quantity;

    public SkuMapping(String skuId, int quantity) {
        validate(skuId, quantity);
        this.skuId = skuId;
        this.quantity = quantity;
    }

    private void validate(String skuId, int quantity) {
        if (skuId == null || skuId.trim().isEmpty()) {
            throw new InvalidSkuMappingException("SKU ID cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new InvalidSkuMappingException("Quantity must be positive");
        }
    }

    @Override
    public String toString() {
        return "SkuMapping{" +
                "skuId='" + skuId + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}