package com.commerce.product.domain.service;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.ProductId;

import java.util.List;

/**
 * 카테고리 관련 도메인 서비스
 */
public interface CategoryService {
    
    /**
     * 카테고리 트리를 구성합니다.
     * 
     * @return 최상위 카테고리부터 시작하는 전체 카테고리 트리
     */
    CategoryTree buildCategoryTree();
    
    /**
     * 특정 상품이 속한 카테고리 목록을 조회합니다.
     * 
     * @param productId 상품 ID
     * @return 상품이 속한 카테고리 목록
     */
    List<Category> getProductCategories(ProductId productId);
    
    /**
     * 상품을 카테고리에 할당합니다.
     * 
     * @param productId 상품 ID
     * @param categoryIds 할당할 카테고리 ID 목록
     */
    void assignProductToCategories(ProductId productId, List<CategoryId> categoryIds);
    
    /**
     * 카테고리의 전체 경로를 조회합니다.
     * 
     * @param categoryId 카테고리 ID
     * @return 최상위부터 해당 카테고리까지의 경로
     */
    List<Category> getCategoryPath(CategoryId categoryId);
    
    /**
     * 카테고리 트리 구조를 나타내는 DTO
     */
    class CategoryTree {
        private final List<CategoryNode> roots;
        
        public CategoryTree(List<CategoryNode> roots) {
            this.roots = roots;
        }
        
        public List<CategoryNode> getRoots() {
            return roots;
        }
    }
    
    /**
     * 카테고리 노드를 나타내는 DTO
     */
    class CategoryNode {
        private final Category category;
        private final List<CategoryNode> children;
        
        public CategoryNode(Category category, List<CategoryNode> children) {
            this.category = category;
            this.children = children;
        }
        
        public Category getCategory() {
            return category;
        }
        
        public List<CategoryNode> getChildren() {
            return children;
        }
    }
}