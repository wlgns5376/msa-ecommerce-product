package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryResponse {
    
    private String categoryId;
    private String name;
    private String parentId;
    private int level;
    private int sortOrder;
    private boolean isActive;
    private String fullPath;
    
    public static CreateCategoryResponse from(Category category) {
        return CreateCategoryResponse.builder()
                .categoryId(category.getId().value())
                .name(category.getName().value())
                .parentId(category.getParentId() != null ? category.getParentId().value() : null)
                .level(category.getLevel())
                .sortOrder(category.getSortOrder())
                .isActive(category.isActive())
                .fullPath(category.getFullPath())
                .build();
    }
}