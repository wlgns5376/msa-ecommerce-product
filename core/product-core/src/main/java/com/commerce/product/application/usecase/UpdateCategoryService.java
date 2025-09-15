package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.InvalidCategoryIdException;
import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.CategoryName;
import com.commerce.product.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateCategoryService implements UpdateCategoryUseCase {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    public UpdateCategoryResponse execute(UpdateCategoryRequest request) {
        CategoryId categoryId = CategoryId.of(request.getCategoryId());
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidCategoryIdException("Category not found: " + request.getCategoryId()));
        
        if (request.getName() != null) {
            // CategoryName 값 객체가 유효성 검사를 수행
            category.updateName(CategoryName.of(request.getName()));
        }
        
        if (request.getSortOrder() != null) {
            // Category 도메인이 sortOrder 유효성 검사를 수행
            category.updateSortOrder(request.getSortOrder());
        }
        
        if (request.getIsActive() != null) {
            if (request.getIsActive()) {
                category.activate();
            } else {
                boolean hasActiveProducts = categoryRepository.hasActiveProducts(categoryId);
                category.setHasActiveProducts(hasActiveProducts);
                category.deactivate();
            }
        }
        
        Category savedCategory = categoryRepository.save(category);
        
        return UpdateCategoryResponse.from(savedCategory);
    }
}