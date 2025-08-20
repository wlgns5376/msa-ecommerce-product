package com.commerce.inventory.application.port.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveBundleStockCommand {
    private String orderId;
    private String reservationId;
    private List<BundleItem> bundleItems;
    private Integer ttlSeconds;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BundleItem {
        private String productOptionId;
        private List<SkuMapping> skuMappings;
        private Integer quantity;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuMapping {
        private String skuId;
        private Integer quantity;
    }
}