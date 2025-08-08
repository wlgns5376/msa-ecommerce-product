package com.commerce.product.domain.service;

import com.commerce.product.domain.model.ProductOption;
import com.commerce.product.domain.model.SkuMapping;

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
}