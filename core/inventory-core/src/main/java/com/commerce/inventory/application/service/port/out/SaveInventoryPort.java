package com.commerce.inventory.application.port.out;

import com.commerce.inventory.domain.model.Inventory;

import java.util.Collection;

public interface SaveInventoryPort {
    void save(Inventory inventory);
    void saveAll(Collection<Inventory> inventories);
}