package com.commerce.inventory.infrastructure.persistence.repository;

import com.commerce.inventory.infrastructure.persistence.entity.ReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, String> {
    
    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.orderId = :orderId AND r.inventoryId = :inventoryId")
    Optional<ReservationJpaEntity> findByOrderIdAndInventoryId(@Param("orderId") String orderId, @Param("inventoryId") String inventoryId);
    
    List<ReservationJpaEntity> findByOrderId(String orderId);
    
    @Query("SELECT r FROM ReservationJpaEntity r WHERE r.id IN :ids")
    List<ReservationJpaEntity> findAllByIdIn(@Param("ids") List<String> ids);
}