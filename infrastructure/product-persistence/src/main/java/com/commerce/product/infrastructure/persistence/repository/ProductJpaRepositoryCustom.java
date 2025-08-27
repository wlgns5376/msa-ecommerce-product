package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface ProductJpaRepositoryCustom {
    Page<ProductJpaEntity> searchProducts(
        String categoryId,
        String keyword,
        Integer minPrice,
        Integer maxPrice,
        Set<ProductStatus> statuses,
        Pageable pageable
    );
}