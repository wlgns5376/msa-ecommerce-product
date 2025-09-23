package com.commerce.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "재고 입고 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveStockRequest {

    @Schema(description = "입고 수량", example = "100", required = true)
    @Positive(message = "입고 수량은 0보다 커야 합니다")
    private int quantity;

    @Schema(description = "참조 번호 (예: 구매 주문 번호)", example = "PO-2024-001", required = true)
    @NotBlank(message = "참조 번호는 필수입니다")
    private String reference;
}