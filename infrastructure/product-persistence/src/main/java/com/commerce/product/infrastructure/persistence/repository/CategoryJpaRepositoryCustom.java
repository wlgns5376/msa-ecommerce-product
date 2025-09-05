package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;

import java.util.List;

public interface CategoryJpaRepositoryCustom {
    /**
     * 리프 카테고리부터 루트까지의 전체 경로를 조회합니다.
     * 재귀 CTE를 사용하여 하위 카테고리부터 상위 카테고리까지 순서대로 조회합니다.
     * 
     * @param leafCategoryId 리프 카테고리 ID
     * @return 카테고리 경로 (하위에서 상위 순서)
     */
    List<CategoryJpaEntity> findCategoryPath(String leafCategoryId);
}