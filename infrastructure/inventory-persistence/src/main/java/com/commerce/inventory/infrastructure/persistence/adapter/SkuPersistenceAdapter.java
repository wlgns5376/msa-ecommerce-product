package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.inventory.application.port.out.LoadSkuPort;
import com.commerce.inventory.application.port.out.SaveSkuPort;
import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuCode;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.infrastructure.persistence.entity.SkuJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.SkuJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SkuPersistenceAdapter implements LoadSkuPort, SaveSkuPort {
    
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
    
    @Override
    public boolean existsByCode(SkuCode code) {
        return skuJpaRepository.existsByCode(code.value());
    }
    
    @Override
    public Sku save(Sku sku) {
        SkuJpaEntity entity = SkuJpaEntity.fromDomainModel(sku);
        SkuJpaEntity savedEntity = skuJpaRepository.save(entity);
        return savedEntity.toDomainModel();
    }
}