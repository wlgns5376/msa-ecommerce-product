package com.commerce.inventory.domain.repository;

import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.product.domain.repository.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface InventoryRepository extends Repository<Inventory, SkuId> {
    
    Optional<Inventory> findBySkuIdWithLock(SkuId skuId);
    
    List<Inventory> findBySkuIds(Set<SkuId> skuIds);
    
    Map<SkuId, Inventory> findBySkuIdsAsMap(Set<SkuId> skuIds);
    
    List<Inventory> findLowStockInventories(int threshold);
}