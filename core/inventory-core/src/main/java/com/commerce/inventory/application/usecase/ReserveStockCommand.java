package com.commerce.inventory.application.usecase;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReserveStockCommand {
    
    private final List<ReservationItem> items;
    private final String orderId;
    private final Integer ttlSeconds;
    
    @Getter
    @Builder
    public static class ReservationItem {
        private final String skuId;
        private final Integer quantity;
    }
}