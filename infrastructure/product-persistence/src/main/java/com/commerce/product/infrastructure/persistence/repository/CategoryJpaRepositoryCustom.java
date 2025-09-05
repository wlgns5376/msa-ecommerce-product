package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;

import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepositoryCustom {
    
    /**
     * ID로 카테고리를 자식들과 함께 조회합니다.
     */
    Optional<CategoryJpaEntity> findByIdWithChildrenQueryDsl(String id);
    
    /**
     * 카테고리에 활성 상품이 있는지 확인합니다.
     */
    boolean hasActiveProductsQueryDsl(String categoryId);
    
    /**
     * 모든 카테고리를 계층 구조로 조회합니다.
     */
    List<CategoryJpaEntity> findAllWithHierarchyQueryDsl();
    
    /**
     * 리프 카테고리부터 루트까지의 경로를 조회합니다.
     */
    List<CategoryJpaEntity> findCategoryPath(String leafCategoryId);
    
    /**
     * 최상위 카테고리 목록을 조회합니다.
     */
    List<CategoryJpaEntity> findRootCategoriesQueryDsl();
    
    /**
     * 특정 부모 카테고리의 하위 카테고리 목록을 조회합니다.
     */
    List<CategoryJpaEntity> findByParentIdQueryDsl(String parentId);
    
    /**
     * 활성 상태의 카테고리 목록을 조회합니다.
     */
    List<CategoryJpaEntity> findActiveCategoriesQueryDsl();
    
    /**
     * 여러 ID로 카테고리들을 조회합니다.
     */
    List<CategoryJpaEntity> findAllByIdInQueryDsl(List<String> categoryIds);
}