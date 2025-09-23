package com.commerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 예약 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "재고 예약 응답")
public class ReserveStockResponseDto {
    
    @Schema(description = "예약 결과 목록")
    private List<ReservationResultDto> reservations;
    
    /**
     * 재고 예약 결과 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "재고 예약 결과")
    public static class ReservationResultDto {
        
        @Schema(description = "예약 ID", example = "RES-001")
        private String reservationId;
        
        @Schema(description = "SKU ID", example = "SKU-001")
        private String skuId;
        
        @Schema(description = "예약 수량", example = "2")
        private Integer quantity;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "예약 만료 시간", example = "2024-01-01T12:00:00")
        private LocalDateTime expiresAt;
        
        @Schema(description = "예약 상태", example = "RESERVED")
        private String status;
    }
}