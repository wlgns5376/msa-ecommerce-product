package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuCode;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.repository.SkuRepository;
import com.commerce.inventory.infrastructure.persistence.entity.SkuJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.SkuJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SkuRepositoryAdapter implements SkuRepository {
    
    private final SkuJpaRepository skuJpaRepository;
    
    @Override
    public Sku save(Sku sku) {
        SkuJpaEntity entity = SkuJpaEntity.fromDomainModel(sku);
        SkuJpaEntity savedEntity = skuJpaRepository.save(entity);
        return savedEntity.toDomainModel();
    }
    
    @Override
    public List<Sku> saveAll(List<Sku> skus) {
        List<SkuJpaEntity> entities = skus.stream()
                .map(SkuJpaEntity::fromDomainModel)
                .collect(Collectors.toList());
        List<SkuJpaEntity> savedEntities = skuJpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(SkuJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Sku> findById(SkuId id) {
        return skuJpaRepository.findById(id.value())
                .map(SkuJpaEntity::toDomainModel);
    }
    
    @Override
    public List<Sku> findAll() {
        return skuJpaRepository.findAll().stream()
                .map(SkuJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(Sku sku) {
        skuJpaRepository.deleteById(sku.getId().value());
    }
    
    @Override
    public void deleteById(SkuId id) {
        skuJpaRepository.deleteById(id.value());
    }
    
    @Override
    public boolean existsById(SkuId id) {
        return skuJpaRepository.existsById(id.value());
    }
    
    @Override
    public Optional<Sku> findByCode(SkuCode code) {
        return skuJpaRepository.findByCode(code.value())
                .map(SkuJpaEntity::toDomainModel);
    }
    
    @Override
    public boolean existsByCode(SkuCode code) {
        return skuJpaRepository.existsByCode(code.value());
    }
}