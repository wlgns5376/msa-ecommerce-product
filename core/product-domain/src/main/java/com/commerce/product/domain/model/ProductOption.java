package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidProductOptionException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode
public class ProductOption implements ValueObject {
    private final String id;
    private final String name;
    private final Money price;
    private final SkuMapping skuMapping;

    private ProductOption(String id, String name, Money price, SkuMapping skuMapping) {
        validate(id, name, price, skuMapping);
        this.id = id;
        this.name = name;
        this.price = price;
        this.skuMapping = skuMapping;
    }

    public static ProductOption single(String name, Money price, String skuId) {
        return new ProductOption(
                UUID.randomUUID().toString(),
                name,
                price,
                SkuMapping.single(skuId)
        );
    }

    public static ProductOption single(String name, Money price, SkuMapping skuMapping) {
        if (skuMapping.isBundle()) {
            throw new InvalidProductOptionException("Single option cannot have bundle SKU mapping");
        }
        return new ProductOption(
                UUID.randomUUID().toString(),
                name,
                price,
                skuMapping
        );
    }

    public static ProductOption bundle(String name, Money price, SkuMapping skuMapping) {
        if (!skuMapping.isBundle()) {
            throw new InvalidProductOptionException("Bundle option must have bundle SKU mapping");
        }
        return new ProductOption(
                UUID.randomUUID().toString(),
                name,
                price,
                skuMapping
        );
    }

    private void validate(String id, String name, Money price, SkuMapping skuMapping) {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidProductOptionException("Option ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidProductOptionException("Option name cannot be null or empty");
        }
        if (price == null) {
            throw new InvalidProductOptionException("Price cannot be null");
        }
        if (skuMapping == null) {
            throw new InvalidProductOptionException("SKU mapping cannot be null");
        }
    }

    public boolean isBundle() {
        return skuMapping.isBundle();
    }

    public String getSingleSkuId() {
        return skuMapping.getSingleSkuId();
    }

    public int getSkuQuantity(String skuId) {
        return skuMapping.getQuantityForSku(skuId);
    }

    @Override
    public String toString() {
        return "ProductOption{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", skuMapping=" + skuMapping +
                '}';
    }
}