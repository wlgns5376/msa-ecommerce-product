package com.commerce.product.infrastructure.persistence.repository;

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
    
    @Query(value = "SELECT DISTINCT p FROM ProductJpaEntity p " +
           "LEFT JOIN FETCH p.options " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND p.deletedAt IS NULL",
           countQuery = "SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND p.deletedAt IS NULL")
    Page<ProductJpaEntity> searchByName(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND p.deletedAt IS NULL")
    long countByName(@Param("keyword") String keyword);
    
    @Query(value = "SELECT DISTINCT p FROM ProductJpaEntity p " +
           "LEFT JOIN FETCH p.options " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND p.status = 'ACTIVE' " +
           "AND p.deletedAt IS NULL",
           countQuery = "SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND p.status = 'ACTIVE' " +
           "AND p.deletedAt IS NULL")
    Page<ProductJpaEntity> searchActiveByName(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT COUNT(DISTINCT p) FROM ProductJpaEntity p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND p.status = 'ACTIVE' " +
           "AND p.deletedAt IS NULL")
    long countActiveByName(@Param("keyword") String keyword);
}