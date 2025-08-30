package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.InvalidCategoryIdException;
import com.commerce.product.domain.exception.InvalidCategoryNameException;
import com.commerce.product.domain.exception.MaxCategoryDepthException;
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
public class CreateCategoryService implements CreateCategoryUseCase {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    public CreateCategoryResponse execute(CreateCategoryRequest request) {
        validateRequest(request);
        
        Category category;
        
        if (request.getParentId() == null) {
            category = createRootCategory(request);
        } else {
            category = createChildCategory(request);
        }
        
        Category savedCategory = categoryRepository.save(category);
        
        return CreateCategoryResponse.from(savedCategory);
    }
    
    private void validateRequest(CreateCategoryRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new InvalidCategoryNameException("Category name cannot be null or empty");
        }
        
        if (request.getName().length() > 100) {
            throw new InvalidCategoryNameException("Category name must not exceed 100 characters");
        }
        
        if (request.getSortOrder() < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
    }
    
    private Category createRootCategory(CreateCategoryRequest request) {
        return Category.createRoot(
                CategoryId.generate(),
                CategoryName.of(request.getName()),
                request.getSortOrder()
        );
    }
    
    private Category createChildCategory(CreateCategoryRequest request) {
        CategoryId parentId = CategoryId.of(request.getParentId());
        
        Category parentCategory = categoryRepository.findById(parentId)
                .orElseThrow(() -> new InvalidCategoryIdException("Parent category not found: " + request.getParentId()));
        
        if (parentCategory.getLevel() >= 3) {
            throw new MaxCategoryDepthException("Maximum category depth is 3. Cannot add child to level " + parentCategory.getLevel() + " category");
        }
        
        int childLevel = parentCategory.getLevel() + 1;
        
        Category childCategory = Category.createChild(
                CategoryId.generate(),
                CategoryName.of(request.getName()),
                parentId,
                childLevel,
                request.getSortOrder()
        );
        
        parentCategory.addChild(childCategory);
        
        return childCategory;
    }
}