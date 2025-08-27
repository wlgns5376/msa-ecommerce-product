package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public class ProductJpaRepositoryCustomImpl implements ProductJpaRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Page<ProductJpaEntity> searchProducts(
            String categoryId,
            String keyword,
            Integer minPrice,
            Integer maxPrice,
            Set<ProductStatus> statuses,
            Pageable pageable) {
        
        // Step 1: Get product IDs with pagination
        String idsQuery = "SELECT DISTINCT p.id FROM ProductJpaEntity p " +
                "LEFT JOIN p.categories c " +
                "WHERE p.deletedAt IS NULL " +
                "AND (:categoryId IS NULL OR c.categoryId = :categoryId) " +
                "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                "AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR EXISTS (SELECT 1 FROM p.options opt WHERE (:minPrice IS NULL OR opt.price >= :minPrice) AND (:maxPrice IS NULL OR opt.price <= :maxPrice))) " +
                "AND (COALESCE(:statuses, null) IS NULL OR p.status IN :statuses)";
        
        TypedQuery<String> idsTypedQuery = entityManager.createQuery(idsQuery, String.class);
        idsTypedQuery.setParameter("categoryId", categoryId);
        idsTypedQuery.setParameter("keyword", keyword);
        idsTypedQuery.setParameter("minPrice", minPrice);
        idsTypedQuery.setParameter("maxPrice", maxPrice);
        idsTypedQuery.setParameter("statuses", statuses);
        idsTypedQuery.setFirstResult((int) pageable.getOffset());
        idsTypedQuery.setMaxResults(pageable.getPageSize());
        
        List<String> productIds = idsTypedQuery.getResultList();
        
        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Step 2: Fetch products with their relations using the IDs
        String fetchQuery = "SELECT DISTINCT p FROM ProductJpaEntity p " +
                "LEFT JOIN FETCH p.options " +
                "LEFT JOIN FETCH p.categories " +
                "WHERE p.id IN :ids " +
                "ORDER BY p.createdAt DESC";
        
        TypedQuery<ProductJpaEntity> fetchTypedQuery = entityManager.createQuery(fetchQuery, ProductJpaEntity.class);
        fetchTypedQuery.setParameter("ids", productIds);
        
        List<ProductJpaEntity> products = fetchTypedQuery.getResultList();
        
        // Count total elements
        String countQuery = "SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p " +
                "LEFT JOIN p.categories c " +
                "WHERE p.deletedAt IS NULL " +
                "AND (:categoryId IS NULL OR c.categoryId = :categoryId) " +
                "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                "AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR EXISTS (SELECT 1 FROM p.options opt WHERE (:minPrice IS NULL OR opt.price >= :minPrice) AND (:maxPrice IS NULL OR opt.price <= :maxPrice))) " +
                "AND (COALESCE(:statuses, null) IS NULL OR p.status IN :statuses)";
        
        TypedQuery<Long> countTypedQuery = entityManager.createQuery(countQuery, Long.class);
        countTypedQuery.setParameter("categoryId", categoryId);
        countTypedQuery.setParameter("keyword", keyword);
        countTypedQuery.setParameter("minPrice", minPrice);
        countTypedQuery.setParameter("maxPrice", maxPrice);
        countTypedQuery.setParameter("statuses", statuses);
        
        Long total = countTypedQuery.getSingleResult();
        
        return new PageImpl<>(products, pageable, total);
    }
}