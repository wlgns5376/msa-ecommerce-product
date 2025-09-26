package com.commerce.product.api.mapper;

import com.commerce.product.api.adapter.in.web.dto.CategoryResponse;
import com.commerce.product.api.adapter.in.web.dto.CreateCategoryApiRequest;
import com.commerce.product.application.usecase.CreateCategoryRequest;
import com.commerce.product.application.usecase.CreateCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    
    public CreateCategoryRequest toCreateCategoryRequest(CreateCategoryApiRequest apiRequest) {
        return CreateCategoryRequest.builder()
                .name(apiRequest.getName())
                .parentId(apiRequest.getParentId())
                .sortOrder(apiRequest.getSortOrder())
                .build();
    }
    
    public CategoryResponse toCategoryResponse(CreateCategoryResponse useCaseResponse) {
        return CategoryResponse.builder()
                .categoryId(useCaseResponse.getCategoryId())
                .name(useCaseResponse.getName())
                .parentId(useCaseResponse.getParentId())
                .level(useCaseResponse.getLevel())
                .sortOrder(useCaseResponse.getSortOrder())
                .active(useCaseResponse.isActive())
                .fullPath(useCaseResponse.getFullPath())
                .build();
    }
}