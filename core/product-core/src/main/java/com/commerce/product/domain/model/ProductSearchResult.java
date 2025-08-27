package com.commerce.product.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductSearchResult(
    ProductId id,
    ProductName name,
    String description,
    ProductType type,
    ProductStatus status,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    List<CategoryId> categoryIds,
    LocalDateTime createdAt
) {}