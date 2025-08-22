package com.commerce.inventory.domain.repository;

import com.commerce.inventory.domain.model.MovementId;
import com.commerce.inventory.domain.model.MovementType;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.model.StockMovement;
import com.commerce.product.domain.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends Repository<StockMovement, MovementId> {
    
    Page<StockMovement> findBySkuId(SkuId skuId, Pageable pageable);
    
    List<StockMovement> findBySkuIdAndType(SkuId skuId, MovementType type);
    
    List<StockMovement> findBySkuIdAndTimestampBetween(
            SkuId skuId, 
            LocalDateTime startTime, 
            LocalDateTime endTime
    );
    
    long countBySkuId(SkuId skuId);
}