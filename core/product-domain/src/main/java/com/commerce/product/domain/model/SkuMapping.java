package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidSkuMappingException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@EqualsAndHashCode
public class SkuMapping implements ValueObject {
    private final Map<String, Integer> mappings;
    private final boolean isBundle;

    private SkuMapping(Map<String, Integer> mappings) {
        validate(mappings);
        this.mappings = Collections.unmodifiableMap(new HashMap<>(mappings));
        this.isBundle = mappings.size() > 1;
    }

    public static SkuMapping single(String skuId) {
        if (skuId == null || skuId.trim().isEmpty()) {
            throw new InvalidSkuMappingException("SKU ID cannot be null or empty");
        }
        
        Map<String, Integer> mapping = new HashMap<>();
        mapping.put(skuId, 1);
        return new SkuMapping(mapping);
    }

    public static SkuMapping bundle(Map<String, Integer> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            throw new InvalidSkuMappingException("Bundle mappings cannot be null or empty");
        }
        if (mappings.size() < 2) {
            throw new InvalidSkuMappingException("Bundle must contain at least 2 SKUs");
        }
        
        return new SkuMapping(mappings);
    }

    private void validate(Map<String, Integer> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            throw new InvalidSkuMappingException("SKU mappings cannot be null or empty");
        }
        
        for (Map.Entry<String, Integer> entry : mappings.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new InvalidSkuMappingException("SKU ID cannot be null or empty");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new InvalidSkuMappingException("Quantity must be positive for SKU: " + entry.getKey());
            }
        }
    }

    public String getSingleSkuId() {
        if (isBundle) {
            throw new IllegalStateException("Cannot get single SKU ID from bundle mapping");
        }
        return mappings.keySet().iterator().next();
    }

    public int getQuantityForSku(String skuId) {
        return mappings.getOrDefault(skuId, 0);
    }

    @Override
    public String toString() {
        return "SkuMapping{" +
                "mappings=" + mappings +
                ", isBundle=" + isBundle +
                '}';
    }
}