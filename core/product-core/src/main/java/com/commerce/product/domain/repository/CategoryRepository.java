package com.commerce.product.domain.repository;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;

import java.util.List;
import java.util.Optional;

/**
 * Category Aggregate의 리포지토리 인터페이스
 */
public interface CategoryRepository extends Repository<Category, CategoryId> {
    
    /**
     * 카테고리를 저장합니다.
     */
    Category save(Category category);
    
    /**
     * ID로 카테고리를 조회합니다.
     */
    Optional<Category> findById(CategoryId id);
    
    /**
     * 최상위 카테고리 목록을 조회합니다.
     */
    List<Category> findRootCategories();
    
    /**
     * 부모 카테고리의 하위 카테고리 목록을 조회합니다.
     */
    List<Category> findByParentId(CategoryId parentId);
    
    /**
     * 활성 상태의 카테고리를 조회합니다.
     */
    List<Category> findActiveCategories();
    
    /**
     * 카테고리 경로를 조회합니다.
     */
    List<Category> findCategoryPath(CategoryId leafCategoryId);
    
    /**
     * 카테고리를 삭제합니다.
     */
    void delete(Category category);
    
    /**
     * 카테고리에 활성 상품이 있는지 확인합니다.
     */
    boolean hasActiveProducts(CategoryId categoryId);
    
    /**
     * 모든 카테고리를 조회합니다.
     */
    List<Category> findAll();
    
    /**
     * 여러 ID로 카테고리들을 조회합니다.
     */
    List<Category> findAllById(List<CategoryId> categoryIds);
}