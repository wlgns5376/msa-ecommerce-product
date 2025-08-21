package com.commerce.inventory.application.port.out;

import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface LoadInventoryPort {
    Optional<Inventory> load(SkuId skuId);
    
    Map<SkuId, Inventory> loadAllByIds(List<SkuId> skuIds);
    
    Map<SkuId, Inventory> loadBySkuIdsWithLock(Set<SkuId> skuIds);
}