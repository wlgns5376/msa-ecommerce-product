package com.commerce.inventory.application.port.out;

import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;

import java.util.Optional;

public interface LoadInventoryPort {
    Optional<Inventory> load(SkuId skuId);
}