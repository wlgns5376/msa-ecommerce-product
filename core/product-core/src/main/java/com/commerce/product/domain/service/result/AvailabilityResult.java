package com.commerce.product.domain.service.result;

public record AvailabilityResult(
    boolean isAvailable,
    int availableQuantity
) {
    public static AvailabilityResult available(int quantity) {
        return new AvailabilityResult(true, quantity);
    }
    
    public static AvailabilityResult unavailable() {
        return new AvailabilityResult(false, 0);
    }
}