package com.commerce.inventory.infrastructure.persistence.repository;

import com.commerce.inventory.infrastructure.persistence.entity.InventoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;

@Repository
public interface InventoryJpaRepository extends JpaRepository<InventoryJpaEntity, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryJpaEntity i WHERE i.skuId IN :skuIds")
    List<InventoryJpaEntity> findAllByIdWithLock(@Param("skuIds") List<String> skuIds);
}