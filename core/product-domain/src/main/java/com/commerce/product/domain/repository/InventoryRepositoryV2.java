package com.commerce.product.domain.repository;

import com.commerce.product.domain.model.inventory.Inventory;
import com.commerce.product.domain.model.inventory.SkuId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InventoryRepositoryV2 extends Repository {
    
    Optional<Inventory> findBySkuId(SkuId skuId);
    
    void save(Inventory inventory);
    
    Map<SkuId, Inventory> findBySkuIds(List<SkuId> skuIds);
}