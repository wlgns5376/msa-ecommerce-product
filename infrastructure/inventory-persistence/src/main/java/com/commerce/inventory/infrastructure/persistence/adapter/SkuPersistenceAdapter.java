package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.inventory.application.port.out.LoadSkuPort;
import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.infrastructure.persistence.entity.SkuJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.SkuJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SkuPersistenceAdapter implements LoadSkuPort {
    
    private final SkuJpaRepository skuJpaRepository;
    
    @Override
    public Optional<Sku> load(SkuId skuId) {
        return skuJpaRepository.findById(skuId.value())
                .map(SkuJpaEntity::toDomainModel);
    }
    
    @Override
    public boolean exists(SkuId skuId) {
        return skuJpaRepository.existsById(skuId.value());
    }
}