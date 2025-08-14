package com.commerce.inventory.infrastructure.persistence.repository;

import com.commerce.inventory.infrastructure.persistence.entity.InventoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryJpaRepository extends JpaRepository<InventoryJpaEntity, String> {
}