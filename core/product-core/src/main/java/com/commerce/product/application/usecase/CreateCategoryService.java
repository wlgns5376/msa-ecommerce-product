package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.InvalidCategoryIdException;
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
        validateSortOrder(request.getSortOrder());
        
        Category category;
        
        if (request.getParentId() == null) {
            category = createRootCategory(request);
        } else {
            category = createChildCategory(request);
        }
        
        Category savedCategory = categoryRepository.save(category);
        
        return CreateCategoryResponse.from(savedCategory);
    }
    
    private void validateSortOrder(int sortOrder) {
        if (sortOrder < 0) {
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
        
        // 부모 카테고리도 저장하여 children 컬렉션 변경사항 반영
        categoryRepository.save(parentCategory);
        
        return childCategory;
    }
}