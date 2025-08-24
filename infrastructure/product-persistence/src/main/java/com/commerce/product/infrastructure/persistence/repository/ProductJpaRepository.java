package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, String> {
    
    @Query("SELECT p FROM ProductJpaEntity p LEFT JOIN FETCH p.options LEFT JOIN FETCH p.categories WHERE p.id = :id")
    Optional<ProductJpaEntity> findByIdWithDetails(@Param("id") String id);
    
    @Query(value = "SELECT DISTINCT p FROM ProductJpaEntity p " +
           "LEFT JOIN FETCH p.options " +
           "LEFT JOIN p.categories c " +
           "WHERE c.categoryId = :categoryId " +
           "AND p.deletedAt IS NULL",
           countQuery = "SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p " +
           "LEFT JOIN p.categories c " +
           "WHERE c.categoryId = :categoryId " +
           "AND p.deletedAt IS NULL")
    Page<ProductJpaEntity> findByCategoryId(@Param("categoryId") String categoryId, Pageable pageable);
    
    @Query(value = "SELECT DISTINCT p FROM ProductJpaEntity p " +
           "LEFT JOIN FETCH p.options " +
           "WHERE p.status = 'ACTIVE' " +
           "AND p.deletedAt IS NULL",
           countQuery = "SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p " +
           "WHERE p.status = 'ACTIVE' " +
           "AND p.deletedAt IS NULL")
    Page<ProductJpaEntity> findActiveProducts(Pageable pageable);
    
    /**
     * 상품 검색 쿼리
     * 
     * 성능 최적화를 위한 권장사항:
     * 1. 함수 기반 인덱스 생성 (PostgreSQL 예시):
     *    CREATE INDEX idx_product_name_lower ON product (LOWER(name));
     * 
     * 2. 대소문자 미구분 Collation 사용 (MySQL 예시):
     *    ALTER TABLE product MODIFY name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
     *    이 경우 LOWER() 함수 제거 가능
     * 
     * 현재는 호환성을 위해 LOWER() 함수를 유지하되, 
     * 프로덕션 환경에서는 위 최적화 방안 중 하나를 적용하는 것을 권장합니다.
     */
    @Query(value = "SELECT p FROM ProductJpaEntity p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND p.deletedAt IS NULL",
           countQuery = "SELECT COUNT(p) FROM ProductJpaEntity p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND p.deletedAt IS NULL")
    Page<ProductJpaEntity> search(@Param("keyword") String keyword, @Param("status") ProductStatus status, Pageable pageable);
    
}