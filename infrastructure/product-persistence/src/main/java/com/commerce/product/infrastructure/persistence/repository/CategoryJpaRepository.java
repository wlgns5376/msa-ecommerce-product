package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, String>, CategoryJpaRepositoryCustom {
    
    /**
     * 최상위 카테고리 목록을 조회합니다.
     */
    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.parentId IS NULL AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC, c.name ASC")
    List<CategoryJpaEntity> findRootCategories();
    
    /**
     * 특정 부모 카테고리의 하위 카테고리 목록을 조회합니다.
     */
    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.parentId = :parentId AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC, c.name ASC")
    List<CategoryJpaEntity> findByParentId(@Param("parentId") String parentId);
    
    /**
     * 활성 상태의 카테고리 목록을 조회합니다.
     */
    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.isActive = true AND c.deletedAt IS NULL ORDER BY c.level ASC, c.sortOrder ASC, c.name ASC")
    List<CategoryJpaEntity> findActiveCategories();
    
    /**
     * ID로 카테고리를 자식들과 함께 조회합니다.
     * QueryDSL을 사용하는 메서드는 CategoryJpaRepositoryCustom 인터페이스에서 제공됩니다.
     */
    default Optional<CategoryJpaEntity> findByIdWithChildren(String id) {
        return findByIdWithChildrenQueryDsl(id);
    }
    
    
    /**
     * 카테고리에 활성 상품이 있는지 확인합니다.
     * QueryDSL을 사용하는 메서드는 CategoryJpaRepositoryCustom 인터페이스에서 제공됩니다.
     */
    default boolean hasActiveProducts(String categoryId) {
        return hasActiveProductsQueryDsl(categoryId);
    }
    
    /**
     * 여러 ID로 카테고리들을 조회합니다.
     */
    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.id IN :categoryIds AND c.deletedAt IS NULL")
    List<CategoryJpaEntity> findAllByIdIn(@Param("categoryIds") List<String> categoryIds);
    
    /**
     * 모든 카테고리를 계층 구조로 조회합니다.
     * QueryDSL을 사용하는 메서드는 CategoryJpaRepositoryCustom 인터페이스에서 제공됩니다.
     */
    default List<CategoryJpaEntity> findAllWithHierarchy() {
        return findAllWithHierarchyQueryDsl();
    }
}