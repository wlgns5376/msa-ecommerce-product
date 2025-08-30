package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignProductToCategoryResponse {
    
    private String productId;
    private List<String> categoryIds;
    private boolean success;
    private String message;
    
    public static AssignProductToCategoryResponse success(Product product) {
        return AssignProductToCategoryResponse.builder()
                .productId(product.getId().value())
                .categoryIds(product.getCategoryIds().stream()
                        .map(CategoryId::value)
                        .collect(Collectors.toList()))
                .success(true)
                .message("Product successfully assigned to categories")
                .build();
    }
    
    public static AssignProductToCategoryResponse failure(String productId, String message) {
        return AssignProductToCategoryResponse.builder()
                .productId(productId)
                .categoryIds(List.of())
                .success(false)
                .message(message)
                .build();
    }
}