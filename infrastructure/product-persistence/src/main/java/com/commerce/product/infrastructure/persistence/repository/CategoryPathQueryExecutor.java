package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 카테고리 경로 조회를 위한 전용 쿼리 실행자
 * 재귀 CTE와 같은 복잡한 네이티브 쿼리를 별도로 관리합니다.
 */
@Component
@RequiredArgsConstructor
public class CategoryPathQueryExecutor {
    
    private final EntityManager entityManager;
    
    // 재귀 CTE 쿼리를 상수로 정의하여 가독성을 높입니다
    private static final String CATEGORY_PATH_QUERY = """
            WITH RECURSIVE category_path AS (
                -- 리프 카테고리에서 시작
                SELECT c.* 
                FROM categories c 
                WHERE c.id = :leafCategoryId 
                  AND c.deleted_at IS NULL
                
                UNION ALL
                
                -- 재귀적으로 부모 카테고리를 조회
                SELECT c.* 
                FROM categories c 
                INNER JOIN category_path cp ON c.id = cp.parent_id
                WHERE c.deleted_at IS NULL
            )
            -- 루트부터 리프까지의 순서로 정렬
            SELECT * FROM category_path 
            ORDER BY level DESC
            """;
    
    /**
     * 리프 카테고리부터 루트까지의 경로를 조회합니다.
     * 
     * @param leafCategoryId 리프 카테고리 ID
     * @return 루트부터 리프까지의 카테고리 경로
     */
    @SuppressWarnings("unchecked")
    public List<CategoryJpaEntity> findCategoryPath(String leafCategoryId) {
        Query query = entityManager.createNativeQuery(CATEGORY_PATH_QUERY, CategoryJpaEntity.class);
        query.setParameter("leafCategoryId", leafCategoryId);
        
        return query.getResultList();
    }
}