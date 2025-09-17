package com.commerce.product.api.adapter.in.web.dto;

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
public class UpdateProductRequest {
    
    @NotBlank(message = "상품명은 필수입니다")
    private String name;
    
    private String description;
    
    @NotNull(message = "버전 정보는 필수입니다")
    private Long version;
    
    public com.commerce.product.application.usecase.UpdateProductRequest toUseCaseRequest(String productId) {
        return com.commerce.product.application.usecase.UpdateProductRequest.builder()
                .productId(productId)
                .name(name)
                .description(description)
                .version(version)
                .build();
    }
}