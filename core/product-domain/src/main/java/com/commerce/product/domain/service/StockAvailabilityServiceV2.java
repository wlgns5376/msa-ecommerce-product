package com.commerce.product.domain.service;

import com.commerce.product.domain.model.SkuMapping;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;

import java.util.concurrent.CompletableFuture;

public interface StockAvailabilityServiceV2 {
    
    /**
     * 단일 옵션의 재고 가용성을 확인합니다.
     */
    CompletableFuture<AvailabilityResult> checkProductOptionAvailability(String optionId);
    
    /**
     * 묶음 옵션의 재고 가용성을 확인합니다.
     * 분산 락을 사용하여 원자성을 보장합니다.
     */
    CompletableFuture<BundleAvailabilityResult> checkBundleAvailability(SkuMapping skuMapping);
    
    /**
     * 묶음 재고 예약을 위한 Saga 패턴 구현
     */
    CompletableFuture<Void> reserveBundleStock(SkuMapping skuMapping, String reservationId);
}