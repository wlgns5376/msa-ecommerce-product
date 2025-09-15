package com.commerce.inventory.application.service.port.out;

import com.commerce.inventory.domain.model.StockMovement;

public interface SaveStockMovementPort {
    void save(StockMovement stockMovement);
}