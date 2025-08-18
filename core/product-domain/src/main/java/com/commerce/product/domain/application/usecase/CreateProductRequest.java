package com.commerce.product.domain.application.usecase;

import com.commerce.product.domain.model.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateProductRequest {
    @NotBlank(message = "Product name is required")
    private final String name;
    private final String description;
    @NotNull(message = "Product type is required")
    private final ProductType type;
}