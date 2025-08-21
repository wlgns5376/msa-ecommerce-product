package com.commerce.inventory.domain.application.usecase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @NotBlank(message = "주문 ID는 필수입니다")
    private String orderId;
    
    @NotBlank(message = "Saga ID는 필수입니다")
    private String sagaId;
    
    @NotEmpty(message = "번들 항목은 최소 1개 이상이어야 합니다")
    @Valid
    private List<BundleItem> bundleItems;
    
    @Positive(message = "TTL은 0보다 커야 합니다")
    private Integer ttlSeconds;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BundleItem {
        @NotBlank(message = "상품 옵션 ID는 필수입니다")
        private String productOptionId;
        
        @NotEmpty(message = "SKU 매핑은 최소 1개 이상이어야 합니다")
        @Valid
        private List<SkuMapping> skuMappings;
        
        @NotNull(message = "번들 수량은 필수입니다")
        @Positive(message = "번들 수량은 0보다 커야 합니다")
        private Integer quantity;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuMapping {
        @NotBlank(message = "SKU ID는 필수입니다")
        private String skuId;
        
        @NotNull(message = "SKU 수량은 필수입니다")
        @Positive(message = "SKU 수량은 0보다 커야 합니다")
        private Integer quantity;
    }
}