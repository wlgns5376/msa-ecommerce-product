package com.commerce.inventory.infrastructure.persistence.repository;

import com.commerce.inventory.infrastructure.persistence.entity.StockMovementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementJpaRepository extends JpaRepository<StockMovementJpaEntity, String> {
}
