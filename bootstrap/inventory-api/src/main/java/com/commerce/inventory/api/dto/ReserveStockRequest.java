package com.commerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

/**
 * 재고 예약 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "재고 예약 요청")
public class ReserveStockRequest {
    
    @NotBlank(message = "주문 ID는 필수입니다")
    @Schema(description = "주문 ID", example = "ORDER-001", required = true)
    private String orderId;
    
    @Min(value = 1, message = "TTL은 최소 1초 이상이어야 합니다")
    @Max(value = 86400, message = "TTL은 최대 24시간(86400초)을 초과할 수 없습니다")
    @Schema(description = "예약 만료 시간(초). 미지정 시 기본값 900초(15분)", example = "900", required = false)
    private Integer ttlSeconds;
    
    @NotEmpty(message = "예약 항목은 최소 1개 이상 필요합니다")
    @Valid
    @Singular("item")
    @JsonProperty("items")
    @Schema(description = "예약 항목 목록", required = true)
    private List<ReservationItem> items;
    
    /**
     * 재고 예약 항목
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "재고 예약 항목")
    public static class ReservationItem {
        
        @NotBlank(message = "SKU ID는 필수입니다")
        @Schema(description = "SKU ID", example = "SKU-001", required = true)
        private String skuId;
        
        @NotNull(message = "수량은 필수입니다")
        @Positive(message = "수량은 1 이상이어야 합니다")
        @Schema(description = "예약 수량", example = "2", required = true)
        private Integer quantity;
    }
}