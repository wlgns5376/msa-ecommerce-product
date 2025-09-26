package com.commerce.product.api.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "카테고리 생성 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryApiRequest {
    
    @Schema(description = "카테고리 이름", example = "Electronics", required = true)
    @NotBlank(message = "Category name is required")
    @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
    private String name;
    
    @Schema(description = "부모 카테고리 ID", example = "cat-001", nullable = true)
    private String parentId;
    
    @Schema(description = "정렬 순서", example = "1", minimum = "0")
    @Min(value = 0, message = "Sort order must be non-negative")
    private int sortOrder;
}