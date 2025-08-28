package com.commerce.product.application.usecase;

/**
 * 카테고리 생성 UseCase 인터페이스
 */
public interface CreateCategoryUseCase {
    CreateCategoryResponse createCategory(CreateCategoryRequest request);
}