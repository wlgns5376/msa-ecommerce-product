package com.commerce.inventory.domain.application.usecase;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReserveStockResponse {
    
    private final List<ReservationResult> reservations;
    
    @Getter
    @Builder
    public static class ReservationResult {
        private final String reservationId;
        private final String skuId;
        private final Integer quantity;
        private final LocalDateTime expiresAt;
        private final String status;
    }
}