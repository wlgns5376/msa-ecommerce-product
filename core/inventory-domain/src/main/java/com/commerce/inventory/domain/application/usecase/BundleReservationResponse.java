package com.commerce.inventory.domain.application.usecase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleReservationResponse {
    private String sagaId;
    private String orderId;
    private BundleReservationStatus status;
    private List<SkuReservation> skuReservations;
    private String failureReason;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuReservation {
        private String skuId;
        private String reservationId;
        private Integer quantity;
        private LocalDateTime expiresAt;
        private SkuReservationStatus status;
    }
}