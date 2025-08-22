package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
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
    
    @Query("SELECT DISTINCT p FROM ProductJpaEntity p " +
           "LEFT JOIN FETCH p.options " +
           "LEFT JOIN p.categories c " +
           "WHERE c.categoryId = :categoryId " +
           "AND p.deletedAt IS NULL")
    List<ProductJpaEntity> findByCategoryId(@Param("categoryId") String categoryId);
    
    @Query("SELECT DISTINCT p FROM ProductJpaEntity p " +
           "LEFT JOIN FETCH p.options " +
           "WHERE p.status = 'ACTIVE' " +
           "AND p.deletedAt IS NULL")
    List<ProductJpaEntity> findActiveProducts();
    
    @Query("SELECT DISTINCT p FROM ProductJpaEntity p " +
           "LEFT JOIN FETCH p.options " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND p.deletedAt IS NULL")
    List<ProductJpaEntity> searchByName(@Param("keyword") String keyword);
}