package com.commerce.product.infrastructure.persistence.dto;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProductSearchResultDto {
    private final String id;
    private final String name;
    private final String description;
    private final ProductType type;
    private final ProductStatus status;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final List<String> categoryIds;
    private final LocalDateTime createdAt;
    
    public ProductSearchResultDto(
        String id,
        String name,
        String description,
        String type,
        String status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = ProductType.valueOf(type);
        this.status = ProductStatus.valueOf(status);
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.categoryIds = List.of();
        this.createdAt = createdAt;
    }
}