package com.commerce.product.application.usecase;

import lombok.Builder;
import lombok.Getter;

/**
 * 카테고리 생성 요청 DTO
 */
@Getter
@Builder
public class CreateCategoryRequest {
    private final String name;
    private final String parentId;
    private final int sortOrder;
    
    public CreateCategoryRequest(String name, String parentId, int sortOrder) {
        this.name = name;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
    }
}