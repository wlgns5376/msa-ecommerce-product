package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ProductJpaRepositoryCustomImpl implements ProductJpaRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private record WhereClauseResult(String whereClause, Map<String, Object> parameters) {}
    
    // JPQL 프로젝션용 내부 DTO
    private record ProductProjection(
        String id,
        String name,
        String description,
        ProductType type,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        LocalDateTime createdAt
    ) {}
    
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
        
        List<String> productIds = isPriceSort 
            ? executeQueryWithPriceSort(idsQuery, whereResult.parameters(), pageable)
            : executeQueryWithoutPriceSort(idsQuery, whereResult.parameters(), pageable);
        
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
            .toList();
        
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
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT new com.commerce.product.infrastructure.persistence.repository.ProductJpaRepositoryCustomImpl$ProductProjection(")
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
        
        TypedQuery<ProductProjection> typedQuery = entityManager.createQuery(query, ProductProjection.class);
        setQueryParameters(typedQuery, whereResult.parameters());
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<ProductProjection> projections = typedQuery.getResultList();
        
        // Convert projections to DTOs
        List<ProductSearchResultDto> results;
        if (!projections.isEmpty()) {
            List<String> productIds = projections.stream()
                .map(ProductProjection::id)
                .toList();
            
            String categoryQuery = "SELECT p.id, c.categoryId FROM ProductJpaEntity p " +
                    "JOIN p.categories c WHERE p.id IN :ids";
            
            List<Object[]> categoryResults = entityManager.createQuery(categoryQuery, Object[].class)
                .setParameter("ids", productIds)
                .getResultList();
            
            // Group categories by product ID
            Map<String, List<String>> categoriesByProductId = categoryResults.stream()
                .collect(Collectors.groupingBy(
                    row -> (String) row[0],
                    Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));
            
            // Convert projections to final DTOs with category IDs
            results = projections.stream()
                .map(projection -> ProductSearchResultDto.builder()
                    .id(projection.id())
                    .name(projection.name())
                    .description(projection.description())
                    .type(projection.type())
                    .status(projection.status())
                    .minPrice(projection.minPrice())
                    .maxPrice(projection.maxPrice())
                    .categoryIds(categoriesByProductId.getOrDefault(projection.id(), List.of()))
                    .createdAt(projection.createdAt())
                    .build())
                .toList();
        } else {
            results = List.of();
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
        
        whereBuilder.append("WHERE p.deletedAt IS NULL ");
        
        if (categoryId != null) {
            whereBuilder.append("AND EXISTS (SELECT 1 FROM p.categories c WHERE c.categoryId = :categoryId) ");
            parameters.put("categoryId", categoryId);
        }
        
        // Note: LOWER() 함수 사용은 인덱스를 활용하지 못해 대규모 데이터셋에서 성능 저하 가능
        // 성능 최적화 방안:
        // 1. PostgreSQL: ILIKE 연산자 사용 (예: p.name ILIKE CONCAT('%', :keyword, '%'))
        // 2. MySQL: 대소문자 구분 없는 컬레이션 사용 (예: utf8mb4_unicode_ci)
        // 3. 함수 기반 인덱스 생성 (예: CREATE INDEX idx_product_name_lower ON product(LOWER(name)))
        // 4. 전문 검색 엔진 사용 고려 (Elasticsearch, Solr 등)
        whereBuilder.append("AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ");
        
        if (minPrice != null || maxPrice != null) {
            whereBuilder.append("AND EXISTS (SELECT 1 FROM p.options opt WHERE (:minPrice IS NULL OR opt.price >= :minPrice) AND (:maxPrice IS NULL OR opt.price <= :maxPrice)) ");
        }
        
        whereBuilder.append("AND p.status IN :statuses ");
        
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
        
        String clauses = sort.stream()
                .map(order -> {
                    String property = "price".equals(order.getProperty())
                            ? "minPrice"
                            : alias + "." + order.getProperty();
                    String direction = order.getDirection().name();
                    
                    // NullHandling 적용
                    String nullHandling = "";
                    if (order.getNullHandling() != null) {
                        switch (order.getNullHandling()) {
                            case NULLS_FIRST:
                                nullHandling = " NULLS FIRST";
                                break;
                            case NULLS_LAST:
                                nullHandling = " NULLS LAST";
                                break;
                        }
                    }
                    
                    return property + " " + direction + nullHandling;
                })
                .collect(Collectors.joining(", "));
        return "ORDER BY " + clauses;
    }
    
    private String buildOrderByClauseForDto(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "";
        }
        
        String clauses = sort.stream()
                .map(order -> {
                    String property = "price".equals(order.getProperty())
                            ? "MIN(opt.price)"
                            : "p." + order.getProperty();
                    String direction = order.getDirection().name();
                    
                    // NullHandling 적용
                    String nullHandling = "";
                    if (order.getNullHandling() != null) {
                        switch (order.getNullHandling()) {
                            case NULLS_FIRST:
                                nullHandling = " NULLS FIRST";
                                break;
                            case NULLS_LAST:
                                nullHandling = " NULLS LAST";
                                break;
                        }
                    }
                    
                    return property + " " + direction + nullHandling;
                })
                .collect(Collectors.joining(", "));
        return "ORDER BY " + clauses;
    }
    
    private List<String> executeQueryWithPriceSort(String query, Map<String, Object> parameters, Pageable pageable) {
        TypedQuery<Object[]> typedQuery = entityManager.createQuery(query, Object[].class);
        setQueryParameters(typedQuery, parameters);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        return typedQuery.getResultList().stream()
            .map(row -> (String) row[0])
            .toList();
    }
    
    private List<String> executeQueryWithoutPriceSort(String query, Map<String, Object> parameters, Pageable pageable) {
        TypedQuery<String> typedQuery = entityManager.createQuery(query, String.class);
        setQueryParameters(typedQuery, parameters);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        return typedQuery.getResultList();
    }
    
    @Override
    public Page<ProductJpaEntity> findBySearchAndFilters(
            String search,
            ProductType type,
            ProductStatus status,
            Pageable pageable) {
        
        // Build query
        StringBuilder queryBuilder = new StringBuilder("SELECT DISTINCT p FROM ProductJpaEntity p ");
        queryBuilder.append("LEFT JOIN FETCH p.options ");
        queryBuilder.append("LEFT JOIN FETCH p.categories ");
        
        List<String> conditions = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        
        // Always exclude deleted products
        conditions.add("p.deletedAt IS NULL");
        
        // Add search condition
        if (search != null && !search.trim().isEmpty()) {
            conditions.add("(LOWER(p.name) LIKE LOWER(:search) OR LOWER(p.description) LIKE LOWER(:search))");
            parameters.put("search", "%" + search.trim() + "%");
        }
        
        // Add type filter
        if (type != null) {
            conditions.add("p.type = :type");
            parameters.put("type", type);
        }
        
        // Add status filter
        if (status != null) {
            conditions.add("p.status = :status");
            parameters.put("status", status);
        }
        
        // Build WHERE clause
        if (!conditions.isEmpty()) {
            queryBuilder.append("WHERE ");
            queryBuilder.append(String.join(" AND ", conditions));
        }
        
        // Create query for fetching products
        TypedQuery<ProductJpaEntity> query = entityManager.createQuery(queryBuilder.toString(), ProductJpaEntity.class);
        setQueryParameters(query, parameters);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        List<ProductJpaEntity> products = query.getResultList();
        
        // Create count query
        StringBuilder countBuilder = new StringBuilder("SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p ");
        if (!conditions.isEmpty()) {
            countBuilder.append("WHERE ");
            countBuilder.append(String.join(" AND ", conditions));
        }
        
        TypedQuery<Long> countQuery = entityManager.createQuery(countBuilder.toString(), Long.class);
        setQueryParameters(countQuery, parameters);
        Long total = countQuery.getSingleResult();
        
        return new PageImpl<>(products, pageable, total);
    }
}