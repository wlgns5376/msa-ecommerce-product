package com.commerce.inventory.application.port.out;

import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuId;

import java.util.Optional;

public interface LoadSkuPort {
    Optional<Sku> load(SkuId skuId);
    boolean exists(SkuId skuId);
}