package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryJpaRepositoryImpl implements CategoryJpaRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private static final String FIND_CATEGORY_PATH_QUERY = """
            WITH RECURSIVE category_path AS (
                SELECT c.*, 0 as path_level 
                FROM categories c 
                WHERE c.id = :leafCategoryId 
                  AND c.deleted_at IS NULL
                
                UNION ALL
                
                SELECT c.*, cp.path_level + 1 
                FROM categories c 
                INNER JOIN category_path cp ON c.id = cp.parent_id
                WHERE c.deleted_at IS NULL
            )
            SELECT * FROM category_path 
            ORDER BY path_level DESC
            """;
    
    @Override
    @SuppressWarnings("unchecked")
    public List<CategoryJpaEntity> findCategoryPath(String leafCategoryId) {
        Query nativeQuery = entityManager.createNativeQuery(
            FIND_CATEGORY_PATH_QUERY, 
            CategoryJpaEntity.class
        );
        nativeQuery.setParameter("leafCategoryId", leafCategoryId);
        
        return nativeQuery.getResultList();
    }
}