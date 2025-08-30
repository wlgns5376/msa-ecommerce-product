package com.commerce.product.application.usecase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignProductToCategoryRequest {
    
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @NotNull(message = "Category IDs list is required")
    @Size(max = 5, message = "Product can be assigned to maximum 5 categories")
    private List<String> categoryIds;
}