package com.commerce.product.domain.repository;

import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.model.ProductOption;

import java.util.List;
import java.util.Optional;

/**
 * Product Aggregate의 리포지토리 인터페이스
 */
public interface ProductRepository extends Repository<Product, ProductId> {
    
    /**
     * 상품을 저장합니다.
     */
    Product save(Product product);
    
    /**
     * ID로 상품을 조회합니다.
     */
    Optional<Product> findById(ProductId id);
    
    /**
     * ID로 상품 기본 정보만 조회합니다. (연관 엔티티 제외)
     */
    Optional<Product> findByIdWithoutAssociations(ProductId id);
    
    /**
     * 카테고리별 상품을 조회합니다.
     */
    List<Product> findByCategory(CategoryId categoryId, int offset, int limit);
    
    /**
     * 상품 옵션 ID로 옵션을 조회합니다.
     */
    Optional<ProductOption> findOptionById(String optionId);
    
    /**
     * 활성 상태의 상품을 조회합니다.
     */
    List<Product> findActiveProducts(int offset, int limit);
    
    /**
     * 상품명으로 검색합니다.
     */
    List<Product> searchByName(String keyword, int offset, int limit);
    
    /**
     * 상품명으로 검색된 전체 개수를 반환합니다.
     */
    long countByName(String keyword);
    
    /**
     * 활성 상태에서 상품명으로 검색합니다.
     */
    List<Product> searchActiveByName(String keyword, int offset, int limit);
    
    /**
     * 활성 상태에서 상품명으로 검색된 전체 개수를 반환합니다.
     */
    long countActiveByName(String keyword);
    
    /**
     * 상품을 삭제합니다.
     */
    void delete(Product product);
    
    /**
     * 모든 상품의 개수를 반환합니다.
     */
    long count();
}