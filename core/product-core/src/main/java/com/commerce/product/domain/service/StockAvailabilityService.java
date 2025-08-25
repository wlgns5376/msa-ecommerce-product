package com.commerce.product.domain.service;

import com.commerce.product.domain.model.ProductOption;
import com.commerce.product.domain.model.SkuMapping;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface StockAvailabilityService {
    
    boolean checkSingleOption(String skuId, int requestedQuantity);
    
    boolean checkBundleOption(ProductOption bundleOption, int requestedQuantity);
    
    CompletableFuture<Map<String, Boolean>> checkMultipleOptions(List<ProductOption> options, Map<String, Integer> quantities);
    
    CompletableFuture<Boolean> reserveStock(String skuId, int quantity, String orderId);
    
    CompletableFuture<Boolean> reserveBundleStock(ProductOption bundleOption, int quantity, String orderId);
    
    void releaseReservation(String reservationId);
    
    int getAvailableQuantity(String skuId);
    
    Map<String, Integer> getBundleAvailableQuantity(ProductOption bundleOption);
    
    /**
     * 단일 옵션의 재고 가용성을 확인합니다.
     */
    CompletableFuture<AvailabilityResult> checkProductOptionAvailability(String optionId);
    
    /**
     * 단일 SKU의 재고 가용성을 확인합니다.
     * N+1 문제를 방지하기 위해 SKU ID를 직접 받아 처리합니다.
     */
    CompletableFuture<AvailabilityResult> checkSingleSkuAvailability(String skuId);
    
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