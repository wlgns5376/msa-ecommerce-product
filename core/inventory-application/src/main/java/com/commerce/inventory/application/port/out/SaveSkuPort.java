package com.commerce.inventory.application.port.out;

import com.commerce.inventory.domain.model.Sku;

/**
 * SKU 저장 포트
 */
public interface SaveSkuPort {
    /**
     * SKU를 저장합니다.
     *
     * @param sku 저장할 SKU
     * @return 저장된 SKU
     */
    Sku save(Sku sku);
}