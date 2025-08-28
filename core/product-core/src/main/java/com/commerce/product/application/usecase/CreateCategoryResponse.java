package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Category;
import lombok.Builder;
import lombok.Getter;

/**
 * 카테고리 생성 응답 DTO
 */
@Getter
@Builder
public class CreateCategoryResponse {
    private final String id;
    private final String name;
    private final String parentId;
    private final int level;
    private final int sortOrder;
    private final boolean isActive;
    
    public static CreateCategoryResponse from(Category category) {
        return CreateCategoryResponse.builder()
                .id(category.getId().value())
                .name(category.getName().value())
                .parentId(category.getParentId() != null ? category.getParentId().value() : null)
                .level(category.getLevel())
                .sortOrder(category.getSortOrder())
                .isActive(category.isActive())
                .build();
    }
}