package com.commerce.product.domain.repository;

import com.commerce.product.domain.model.inventory.Inventory;
import com.commerce.product.domain.model.inventory.SkuId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public interface InventoryRepository extends Repository {
    
    int getAvailableQuantity(String skuId);
    
    String reserveStock(String skuId, int quantity, String orderId);
    
    void releaseReservation(String reservationId);
    
    Optional<Inventory> findBySkuId(SkuId skuId);
    
    void save(Inventory inventory);
    
    Map<SkuId, Inventory> findBySkuIds(List<SkuId> skuIds);
}