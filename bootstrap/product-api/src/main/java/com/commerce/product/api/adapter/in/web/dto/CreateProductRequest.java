package com.commerce.product.api.adapter.in.web.dto;

import com.commerce.product.domain.model.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    
    @NotBlank(message = "상품명은 필수입니다")
    private String name;
    
    private String description;
    
    @NotNull(message = "상품 타입은 필수입니다")
    private ProductType type;
    
    public com.commerce.product.application.usecase.CreateProductRequest toUseCaseRequest() {
        return com.commerce.product.application.usecase.CreateProductRequest.builder()
                .name(this.name)
                .description(this.description)
                .type(this.type)
                .build();
    }
}