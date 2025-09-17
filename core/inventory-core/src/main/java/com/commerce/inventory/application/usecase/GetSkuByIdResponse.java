package com.commerce.inventory.application.usecase;

import com.commerce.inventory.domain.model.Sku;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class GetSkuByIdResponse {
    
    private final String id;
    private final String code;
    private final String name;
    private final String description;
    private final BigDecimal weight;
    private final BigDecimal volume;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long version;
    
    public static GetSkuByIdResponse from(Sku sku) {
        return GetSkuByIdResponse.builder()
            .id(sku.getId().value())
            .code(sku.getCode().value())
            .name(sku.getName())
            .description(sku.getDescription())
            .weight(sku.getWeight() != null ? BigDecimal.valueOf(sku.getWeight().value()) : null)
            .volume(sku.getVolume() != null ? BigDecimal.valueOf(sku.getVolume().value()) : null)
            .createdAt(sku.getCreatedAt())
            .updatedAt(sku.getUpdatedAt())
            .version(sku.getVersion())
            .build();
    }
}