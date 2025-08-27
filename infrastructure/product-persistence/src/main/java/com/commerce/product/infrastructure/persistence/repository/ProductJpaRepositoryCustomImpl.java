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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        // price 정렬이 필요한 경우 MIN 가격을 계산
        boolean isPriceSort = pageable.getSort().stream()
            .anyMatch(order -> "price".equals(order.getProperty()));
        
        StringBuilder idsQueryBuilder = new StringBuilder();
        if (isPriceSort) {
            idsQueryBuilder.append("SELECT DISTINCT p.id, MIN(opt.price) as minPrice FROM ProductJpaEntity p ")
                    .append("LEFT JOIN p.options opt ");
        } else {
            idsQueryBuilder.append("SELECT DISTINCT p.id FROM ProductJpaEntity p ");
        }
        
        // categoryId가 있을 때만 categories 조인
        if (categoryId != null) {
            idsQueryBuilder.append("LEFT JOIN p.categories c ");
        }
        
        idsQueryBuilder.append("WHERE p.deletedAt IS NULL ");
        
        if (categoryId != null) {
            idsQueryBuilder.append("AND c.categoryId = :categoryId ");
        }
        
        idsQueryBuilder.append("AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ")
                .append("AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR EXISTS (SELECT 1 FROM p.options opt WHERE (:minPrice IS NULL OR opt.price >= :minPrice) AND (:maxPrice IS NULL OR opt.price <= :maxPrice))) ")
                .append("AND p.status IN :statuses");
        
        // price 정렬인 경우 GROUP BY 추가
        if (isPriceSort) {
            idsQueryBuilder.append(" GROUP BY p.id");
        }
        
        // Add dynamic sort
        String orderByClause = buildOrderByClause(pageable.getSort(), "p", isPriceSort);
        if (!orderByClause.isEmpty()) {
            idsQueryBuilder.append(" ").append(orderByClause);
        }
        
        String idsQuery = idsQueryBuilder.toString();
        
        TypedQuery<?> idsTypedQuery = isPriceSort 
            ? entityManager.createQuery(idsQuery, Object[].class)
            : entityManager.createQuery(idsQuery, String.class);
        idsTypedQuery.setParameter("categoryId", categoryId);
        idsTypedQuery.setParameter("keyword", keyword);
        idsTypedQuery.setParameter("minPrice", minPrice);
        idsTypedQuery.setParameter("maxPrice", maxPrice);
        idsTypedQuery.setParameter("statuses", statuses);
        idsTypedQuery.setFirstResult((int) pageable.getOffset());
        idsTypedQuery.setMaxResults(pageable.getPageSize());
        
        List<String> productIds;
        if (isPriceSort) {
            @SuppressWarnings("unchecked")
            List<Object[]> results = (List<Object[]>) idsTypedQuery.getResultList();
            productIds = results.stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());
        } else {
            @SuppressWarnings("unchecked")
            List<String> results = (List<String>) idsTypedQuery.getResultList();
            productIds = results;
        }
        
        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Step 2: Fetch products with their relations using the IDs
        String fetchQuery = "SELECT DISTINCT p FROM ProductJpaEntity p " +
                "LEFT JOIN FETCH p.options " +
                "LEFT JOIN FETCH p.categories " +
                "WHERE p.id IN :ids";
        
        TypedQuery<ProductJpaEntity> fetchTypedQuery = entityManager.createQuery(fetchQuery, ProductJpaEntity.class);
        fetchTypedQuery.setParameter("ids", productIds);
        
        List<ProductJpaEntity> fetchedProducts = fetchTypedQuery.getResultList();
        
        // ID 순서에 따라 정렬 유지
        Map<String, ProductJpaEntity> productMap = fetchedProducts.stream()
            .collect(Collectors.toMap(
                ProductJpaEntity::getId,
                product -> product
            ));
        
        List<ProductJpaEntity> products = productIds.stream()
            .map(productMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // Count total elements
        StringBuilder countQueryBuilder = new StringBuilder("SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p ");
        
        // categoryId가 있을 때만 categories 조인
        if (categoryId != null) {
            countQueryBuilder.append("LEFT JOIN p.categories c ");
        }
        
        countQueryBuilder.append("WHERE p.deletedAt IS NULL ");
        
        if (categoryId != null) {
            countQueryBuilder.append("AND c.categoryId = :categoryId ");
        }
        
        countQueryBuilder.append("AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ")
                .append("AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR EXISTS (SELECT 1 FROM p.options opt WHERE (:minPrice IS NULL OR opt.price >= :minPrice) AND (:maxPrice IS NULL OR opt.price <= :maxPrice))) ")
                .append("AND p.status IN :statuses");
        
        String countQuery = countQueryBuilder.toString();
        
        TypedQuery<Long> countTypedQuery = entityManager.createQuery(countQuery, Long.class);
        countTypedQuery.setParameter("categoryId", categoryId);
        countTypedQuery.setParameter("keyword", keyword);
        countTypedQuery.setParameter("minPrice", minPrice);
        countTypedQuery.setParameter("maxPrice", maxPrice);
        countTypedQuery.setParameter("statuses", statuses);
        
        Long total = countTypedQuery.getSingleResult();
        
        return new PageImpl<>(products, pageable, total);
    }
    
    private String buildOrderByClause(Sort sort, String alias, boolean isPriceSort) {
        if (sort == null || sort.isUnsorted()) {
            return "";
        }
        
        StringBuilder orderBy = new StringBuilder("ORDER BY ");
        sort.forEach(order -> {
            if (orderBy.length() > 9) { // "ORDER BY " has 9 characters
                orderBy.append(", ");
            }
            
            // price 정렬인 경우 minPrice 별칭 사용
            if ("price".equals(order.getProperty())) {
                orderBy.append("minPrice");
            } else {
                orderBy.append(alias).append(".").append(order.getProperty());
            }
            orderBy.append(" ").append(order.getDirection().name());
        });
        
        return orderBy.toString();
    }
}