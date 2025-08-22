package com.commerce.inventory.infrastructure.persistence.repository;

import com.commerce.inventory.infrastructure.persistence.entity.SkuJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkuJpaRepository extends JpaRepository<SkuJpaEntity, String> {
    boolean existsByCode(String code);
}