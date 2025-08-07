package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidProductOptionException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class ProductOption implements ValueObject {
    private final String id;
    private final String name;
    private final Money price;
    private final List<SkuMapping> skuMappings;

    public ProductOption(String id, String name, Money price, List<SkuMapping> skuMappings) {
        validate(id, name, price, skuMappings);
        this.id = id;
        this.name = name;
        this.price = price;
        this.skuMappings = Collections.unmodifiableList(skuMappings);
    }

    public static ProductOption create(String name, Money price, List<SkuMapping> skuMappings) {
        return new ProductOption(UUID.randomUUID().toString(), name, price, skuMappings);
    }

    private void validate(String id, String name, Money price, List<SkuMapping> skuMappings) {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidProductOptionException("Option ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidProductOptionException("Option name cannot be null or empty");
        }
        if (price == null) {
            throw new InvalidProductOptionException("Price cannot be null");
        }
        if (skuMappings == null || skuMappings.isEmpty()) {
            throw new InvalidProductOptionException("Option must have at least one SKU mapping");
        }
    }

    public boolean hasMultipleSkus() {
        return skuMappings.size() > 1;
    }

    public int getTotalSkuQuantity(String skuId) {
        return skuMappings.stream()
                .filter(mapping -> mapping.getSkuId().equals(skuId))
                .mapToInt(SkuMapping::getQuantity)
                .sum();
    }
}