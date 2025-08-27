package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class ProductJpaRepositoryCustomImpl implements ProductJpaRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Page<ProductJpaEntity> searchProducts(
            String categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Set<ProductStatus> statuses,
            Pageable pageable) {
        
        // Step 1: Get product IDs with pagination
        StringBuilder idsQueryBuilder = new StringBuilder("SELECT DISTINCT p.id FROM ProductJpaEntity p " +
                "LEFT JOIN p.categories c " +
                "WHERE p.deletedAt IS NULL " +
                "AND (:categoryId IS NULL OR c.categoryId = :categoryId) " +
                "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                "AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR EXISTS (SELECT 1 FROM p.options opt WHERE (:minPrice IS NULL OR opt.price >= :minPrice) AND (:maxPrice IS NULL OR opt.price <= :maxPrice))) " +
                "AND (COALESCE(:statuses, null) IS NULL OR p.status IN :statuses)");
        
        // Add dynamic sort
        String orderByClause = buildOrderByClause(pageable.getSort(), "p");
        if (!orderByClause.isEmpty()) {
            idsQueryBuilder.append(" ").append(orderByClause);
        }
        
        String idsQuery = idsQueryBuilder.toString();
        
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
        StringBuilder fetchQueryBuilder = new StringBuilder("SELECT DISTINCT p FROM ProductJpaEntity p " +
                "LEFT JOIN FETCH p.options " +
                "LEFT JOIN FETCH p.categories " +
                "WHERE p.id IN :ids");
        
        // Add the same ordering to maintain the sort order
        if (!orderByClause.isEmpty()) {
            fetchQueryBuilder.append(" ").append(orderByClause);
        }
        
        String fetchQuery = fetchQueryBuilder.toString();
        
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
    
    private String buildOrderByClause(Sort sort, String alias) {
        if (sort == null || sort.isUnsorted()) {
            return "";
        }
        
        StringBuilder orderBy = new StringBuilder("ORDER BY ");
        sort.forEach(order -> {
            if (orderBy.length() > 9) { // "ORDER BY " has 9 characters
                orderBy.append(", ");
            }
            orderBy.append(alias).append(".").append(order.getProperty());
            orderBy.append(" ").append(order.getDirection().name());
        });
        
        return orderBy.toString();
    }
}