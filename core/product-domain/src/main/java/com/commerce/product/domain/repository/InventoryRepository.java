package com.commerce.product.domain.repository;

import java.util.concurrent.locks.Lock;

public interface InventoryRepository extends Repository {
    
    int getAvailableQuantity(String skuId);
    
    String reserveStock(String skuId, int quantity, String orderId);
    
    void releaseReservation(String reservationId);
}