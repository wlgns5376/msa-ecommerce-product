package com.commerce.inventory.application.service.port.out;

import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuCode;
import com.commerce.inventory.domain.model.SkuId;

import java.util.Optional;

public interface LoadSkuPort {
    Optional<Sku> load(SkuId skuId);
    boolean exists(SkuId skuId);
    boolean existsByCode(SkuCode code);
}