package com.commerce.inventory.application.port.out;

import com.commerce.inventory.domain.model.Inventory;

public interface SaveInventoryPort {
    void save(Inventory inventory);
}