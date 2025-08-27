package com.commerce.product.test.helper;

import com.commerce.product.domain.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductSearchResultTestBuilder {
    private ProductId id = new ProductId(UUID.randomUUID().toString());
    private ProductName name = new ProductName("Test Product");
    private String description = "Test product description";
    private ProductType type = ProductType.NORMAL;
    private ProductStatus status = ProductStatus.ACTIVE;
    private BigDecimal minPrice = new BigDecimal("10000");
    private BigDecimal maxPrice = new BigDecimal("20000");
    private List<CategoryId> categoryIds = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public static ProductSearchResultTestBuilder aProductSearchResult() {
        return new ProductSearchResultTestBuilder();
    }
    
    public ProductSearchResultTestBuilder withId(String id) {
        try {
            UUID.fromString(id);
            this.id = new ProductId(id);
        } catch (IllegalArgumentException e) {
            // UUID가 아닌 경우 새로운 UUID 생성
            this.id = ProductId.generate();
        }
        return this;
    }
    
    public ProductSearchResultTestBuilder withName(String name) {
        this.name = new ProductName(name);
        return this;
    }
    
    public ProductSearchResultTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }
    
    public ProductSearchResultTestBuilder withType(ProductType type) {
        this.type = type;
        return this;
    }
    
    public ProductSearchResultTestBuilder withStatus(ProductStatus status) {
        this.status = status;
        return this;
    }
    
    public ProductSearchResultTestBuilder withMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
        return this;
    }
    
    public ProductSearchResultTestBuilder withMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
        return this;
    }
    
    public ProductSearchResultTestBuilder withCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds.stream()
            .map(CategoryId::new)
            .toList();
        return this;
    }
    
    public ProductSearchResultTestBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    public ProductSearchResult build() {
        return new ProductSearchResult(
            id,
            name,
            description,
            type,
            status,
            minPrice,
            maxPrice,
            categoryIds,
            createdAt
        );
    }
}