package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.CannotDeactivateCategoryException;
import com.commerce.product.domain.exception.InvalidCategoryIdException;
import com.commerce.product.domain.exception.InvalidCategoryNameException;
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
            validateName(request.getName());
            category.updateName(CategoryName.of(request.getName()));
        }
        
        if (request.getSortOrder() != null) {
            validateSortOrder(request.getSortOrder());
            category.updateSortOrder(request.getSortOrder());
        }
        
        if (request.getIsActive() != null) {
            if (request.getIsActive()) {
                category.activate();
            } else {
                boolean hasActiveProducts = categoryRepository.hasActiveProducts(categoryId);
                if (hasActiveProducts) {
                    throw new CannotDeactivateCategoryException("Cannot deactivate category with active products");
                }
                category.deactivate();
            }
        }
        
        Category savedCategory = categoryRepository.save(category);
        
        return UpdateCategoryResponse.from(savedCategory);
    }
    
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidCategoryNameException("Category name cannot be null or empty");
        }
        
        if (name.length() > 100) {
            throw new InvalidCategoryNameException("Category name must not exceed 100 characters");
        }
    }
    
    private void validateSortOrder(Integer sortOrder) {
        if (sortOrder < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
    }
}