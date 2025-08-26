package com.commerce.product.infrastructure.persistence.specification;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecification {
    
    private ProductSpecification() { }
    
    public static Specification<ProductJpaEntity> withKeywordAndStatus(String keyword, ProductStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // deletedAt이 null인 조건
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            
            // 키워드 검색 조건
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + keyword.trim().toLowerCase() + "%"
                ));
            }
            
            // 상태 필터 조건
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    public static Specification<ProductJpaEntity> withCategoryId(String categoryId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // deletedAt이 null인 조건
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            
            // 카테고리 조건
            predicates.add(criteriaBuilder.equal(
                root.join("categories").get("categoryId"), 
                categoryId
            ));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    public static Specification<ProductJpaEntity> activeProducts() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // deletedAt이 null인 조건
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            
            // 상태가 ACTIVE인 조건
            predicates.add(criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}