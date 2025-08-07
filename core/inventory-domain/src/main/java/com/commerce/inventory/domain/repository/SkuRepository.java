package com.commerce.inventory.domain.repository;

import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuCode;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.product.domain.repository.Repository;

import java.util.Optional;

public interface SkuRepository extends Repository<Sku, SkuId> {
    
    Optional<Sku> findByCode(SkuCode code);
    
    boolean existsByCode(SkuCode code);
}