package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.CategoryName;
import com.commerce.product.domain.repository.CategoryRepository;
import com.commerce.product.domain.exception.InvalidCategoryLevelException;
import com.commerce.product.domain.exception.InvalidCategoryNameException;
import com.commerce.product.domain.exception.ProductDomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

/**
 * 카테고리 생성 UseCase 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateCategoryService implements CreateCategoryUseCase {
    
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    public CreateCategoryResponse createCategory(CreateCategoryRequest request) {
        validateRequest(request);
        
        Category category;
        
        if (request.getParentId() == null) {
            // 루트 카테고리 생성
            category = createRootCategory(request);
        } else {
            // 하위 카테고리 생성
            category = createChildCategory(request);
        }
        
        Category savedCategory = categoryRepository.save(category);
        
        // 도메인 이벤트 발행
        publishDomainEvents(savedCategory);
        
        return CreateCategoryResponse.from(savedCategory);
    }
    
    private void validateRequest(CreateCategoryRequest request) {
        // 이름 검증
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new InvalidCategoryNameException("Category name cannot be null or empty");
        }
        
        if (request.getName().length() > 100) {
            throw new InvalidCategoryNameException("Category name cannot exceed 100 characters");
        }
        
        // 정렬 순서 검증
        if (request.getSortOrder() < 0) {
            throw new IllegalArgumentException("Sort order must be positive");
        }
    }
    
    private Category createRootCategory(CreateCategoryRequest request) {
        CategoryId categoryId = new CategoryId(UUID.randomUUID().toString());
        CategoryName categoryName = new CategoryName(request.getName());
        
        return Category.createRoot(categoryId, categoryName, request.getSortOrder());
    }
    
    private Category createChildCategory(CreateCategoryRequest request) {
        CategoryId parentId = new CategoryId(request.getParentId());
        
        // 부모 카테고리 조회
        Category parentCategory = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ProductDomainException("Parent category not found: " + request.getParentId()));
        
        // 레벨 확인 (부모가 3레벨이면 자식을 생성할 수 없음)
        if (parentCategory.getLevel() >= 3) {
            throw new InvalidCategoryLevelException("Maximum category level is 3");
        }
        
        CategoryId categoryId = new CategoryId(UUID.randomUUID().toString());
        CategoryName categoryName = new CategoryName(request.getName());
        int childLevel = parentCategory.getLevel() + 1;
        
        return Category.createChild(categoryId, categoryName, parentId, childLevel, request.getSortOrder());
    }
    
    private void publishDomainEvents(Category category) {
        category.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}