package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateProductRequest {
    @NotBlank(message = "상품명은 필수입니다")
    private final String name;
    private final String description;
    @NotNull(message = "상품 타입은 필수입니다")
    private final ProductType type;
}
