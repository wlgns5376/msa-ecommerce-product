package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Set;

public interface ProductJpaRepositoryCustom {
    Page<ProductJpaEntity> searchProducts(
        String categoryId,
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Set<ProductStatus> statuses,
        Pageable pageable
    );
}