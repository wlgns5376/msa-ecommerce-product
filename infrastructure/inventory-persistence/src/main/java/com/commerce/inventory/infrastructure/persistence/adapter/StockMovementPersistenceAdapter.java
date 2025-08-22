package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.inventory.application.service.port.out.SaveStockMovementPort;
import com.commerce.inventory.domain.model.StockMovement;
import com.commerce.inventory.infrastructure.persistence.entity.StockMovementJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockMovementPersistenceAdapter implements SaveStockMovementPort {
    
    private final StockMovementJpaRepository stockMovementJpaRepository;
    
    @Override
    @Transactional
    public void save(StockMovement stockMovement) {
        StockMovementJpaEntity entity = StockMovementJpaEntity.fromDomainModel(stockMovement);
        stockMovementJpaRepository.save(entity);
    }
}