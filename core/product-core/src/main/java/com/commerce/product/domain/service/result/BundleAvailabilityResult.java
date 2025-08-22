package com.commerce.product.domain.service.result;

import java.util.List;

public record BundleAvailabilityResult(
    boolean isAvailable,
    int availableSets,
    List<SkuAvailabilityDetail> details
) {
    public record SkuAvailabilityDetail(
        String skuId,
        int requiredQuantity,
        int availableQuantity,
        int availableSets
    ) {}
    
    public static BundleAvailabilityResult available(int availableSets, List<SkuAvailabilityDetail> details) {
        return new BundleAvailabilityResult(true, availableSets, details);
    }
    
    public static BundleAvailabilityResult unavailable(List<SkuAvailabilityDetail> details) {
        return new BundleAvailabilityResult(false, 0, details);
    }
}