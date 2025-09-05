package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, String>, CategoryJpaRepositoryCustom {
    
    /**
     * 최상위 카테고리 목록을 조회합니다.
     */
    default List<CategoryJpaEntity> findRootCategories() {
        return findRootCategoriesQueryDsl();
    }
    
    /**
     * 특정 부모 카테고리의 하위 카테고리 목록을 조회합니다.
     */
    default List<CategoryJpaEntity> findByParentId(String parentId) {
        return findByParentIdQueryDsl(parentId);
    }
    
    /**
     * 활성 상태의 카테고리 목록을 조회합니다.
     */
    default List<CategoryJpaEntity> findActiveCategories() {
        return findActiveCategoriesQueryDsl();
    }
    
    /**
     * ID로 카테고리를 자식들과 함께 조회합니다.
     */
    default Optional<CategoryJpaEntity> findByIdWithChildren(String id) {
        return findByIdWithChildrenQueryDsl(id);
    }
    
    /**
     * 카테고리에 활성 상품이 있는지 확인합니다.
     */
    default boolean hasActiveProducts(String categoryId) {
        return hasActiveProductsQueryDsl(categoryId);
    }
    
    /**
     * 여러 ID로 카테고리들을 조회합니다.
     */
    default List<CategoryJpaEntity> findAllByIdIn(List<String> categoryIds) {
        return findAllByIdInQueryDsl(categoryIds);
    }
    
    /**
     * 모든 카테고리를 계층 구조로 조회합니다.
     */
    default List<CategoryJpaEntity> findAllWithHierarchy() {
        return findAllWithHierarchyQueryDsl();
    }
}