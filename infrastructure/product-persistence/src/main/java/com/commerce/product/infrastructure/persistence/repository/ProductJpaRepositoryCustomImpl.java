package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.infrastructure.persistence.dto.ProductSearchResultDto;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class ProductJpaRepositoryCustomImpl implements ProductJpaRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private record WhereClauseResult(String whereClause, Map<String, Object> parameters) {}
    
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
        
        // Build WHERE clause using helper method
        WhereClauseResult whereResult = buildWhereClause(categoryId, keyword, minPrice, maxPrice, statuses);
        idsQueryBuilder.append(whereResult.whereClause());
        
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
        setQueryParameters(idsTypedQuery, whereResult.parameters());
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
        Long total = countTotalElements(categoryId, keyword, minPrice, maxPrice, statuses);
        
        return new PageImpl<>(products, pageable, total);
    }
    
    @Override
    public Page<ProductSearchResultDto> searchProductsWithDto(
            String categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Set<ProductStatus> statuses,
            Pageable pageable) {
        
        // Step 1: Get product search results with pagination
        boolean isPriceSort = pageable.getSort().stream()
            .anyMatch(order -> "price".equals(order.getProperty()));
        
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT new com.commerce.product.infrastructure.persistence.dto.ProductSearchResultDto(")
                .append("p.id, p.name, p.description, p.type, p.status, ")
                .append("MIN(opt.price), MAX(opt.price), p.createdAt) ")
                .append("FROM ProductJpaEntity p ")
                .append("LEFT JOIN p.options opt ");
        
        // Build WHERE clause using helper method
        WhereClauseResult whereResult = buildWhereClause(categoryId, keyword, minPrice, maxPrice, statuses);
        queryBuilder.append(whereResult.whereClause());
        
        // GROUP BY for aggregation
        queryBuilder.append(" GROUP BY p.id, p.name, p.description, p.type, p.status, p.createdAt");
        
        // Add dynamic sort
        String orderByClause = buildOrderByClauseForDto(pageable.getSort());
        if (!orderByClause.isEmpty()) {
            queryBuilder.append(" ").append(orderByClause);
        }
        
        String query = queryBuilder.toString();
        
        TypedQuery<ProductSearchResultDto> typedQuery = entityManager.createQuery(query, ProductSearchResultDto.class);
        setQueryParameters(typedQuery, whereResult.parameters());
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<ProductSearchResultDto> results = typedQuery.getResultList();
        
        // Fetch category IDs for the results
        if (!results.isEmpty()) {
            List<String> productIds = results.stream()
                .map(ProductSearchResultDto::getId)
                .collect(Collectors.toList());
            
            String categoryQuery = "SELECT p.id, c.categoryId FROM ProductJpaEntity p " +
                    "JOIN p.categories c WHERE p.id IN :ids";
            
            List<Object[]> categoryResults = entityManager.createQuery(categoryQuery, Object[].class)
                .setParameter("ids", productIds)
                .getResultList();
            
            // Group categories by product ID
            Map<String, List<String>> categoriesByProductId = new HashMap<>();
            for (Object[] row : categoryResults) {
                String productId = (String) row[0];
                String categoryIdValue = (String) row[1];
                categoriesByProductId.computeIfAbsent(productId, k -> new ArrayList<>())
                    .add(categoryIdValue);
            }
            
            // Update results with category IDs
            results = results.stream()
                .map(dto -> ProductSearchResultDto.builder()
                    .id(dto.getId())
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .type(dto.getType())
                    .status(dto.getStatus())
                    .minPrice(dto.getMinPrice())
                    .maxPrice(dto.getMaxPrice())
                    .categoryIds(categoriesByProductId.getOrDefault(dto.getId(), List.of()))
                    .createdAt(dto.getCreatedAt())
                    .build())
                .collect(Collectors.toList());
        }
        
        // Count total elements
        Long total = countTotalElements(categoryId, keyword, minPrice, maxPrice, statuses);
        
        return new PageImpl<>(results, pageable, total);
    }
    
    private WhereClauseResult buildWhereClause(
            String categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Set<ProductStatus> statuses) {
        
        StringBuilder whereBuilder = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();
        
        // categoryId가 있을 때만 categories 조인
        if (categoryId != null) {
            whereBuilder.append("LEFT JOIN p.categories c ");
        }
        
        whereBuilder.append("WHERE p.deletedAt IS NULL ");
        
        if (categoryId != null) {
            whereBuilder.append("AND c.categoryId = :categoryId ");
            parameters.put("categoryId", categoryId);
        }
        
        whereBuilder.append("AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ")
                .append("AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR EXISTS (SELECT 1 FROM p.options opt WHERE (:minPrice IS NULL OR opt.price >= :minPrice) AND (:maxPrice IS NULL OR opt.price <= :maxPrice))) ")
                .append("AND p.status IN :statuses");
        
        parameters.put("keyword", keyword);
        parameters.put("minPrice", minPrice);
        parameters.put("maxPrice", maxPrice);
        parameters.put("statuses", statuses);
        
        return new WhereClauseResult(whereBuilder.toString(), parameters);
    }
    
    private void setQueryParameters(TypedQuery<?> query, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }
    
    private Long countTotalElements(
            String categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Set<ProductStatus> statuses) {
        
        StringBuilder countQueryBuilder = new StringBuilder("SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p ");
        
        WhereClauseResult whereResult = buildWhereClause(categoryId, keyword, minPrice, maxPrice, statuses);
        countQueryBuilder.append(whereResult.whereClause());
        
        TypedQuery<Long> countQuery = entityManager.createQuery(countQueryBuilder.toString(), Long.class);
        setQueryParameters(countQuery, whereResult.parameters());
        
        return countQuery.getSingleResult();
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
    
    private String buildOrderByClauseForDto(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "";
        }
        
        StringBuilder orderBy = new StringBuilder("ORDER BY ");
        sort.forEach(order -> {
            if (orderBy.length() > 9) { // "ORDER BY " has 9 characters
                orderBy.append(", ");
            }
            
            // price 정렬인 경우 MIN(opt.price) 사용
            if ("price".equals(order.getProperty())) {
                orderBy.append("MIN(opt.price)");
            } else {
                orderBy.append("p.").append(order.getProperty());
            }
            orderBy.append(" ").append(order.getDirection().name());
        });
        
        return orderBy.toString();
    }
}