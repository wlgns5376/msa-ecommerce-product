package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.InvalidCategoryIdException;
import com.commerce.product.domain.exception.MaxCategoryLimitException;
import com.commerce.product.domain.exception.ProductNotFoundException;
import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.repository.CategoryRepository;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignProductToCategoryService implements AssignProductToCategoryUseCase {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    
    @Override
    public AssignProductToCategoryResponse execute(AssignProductToCategoryRequest request) {
        validateRequest(request);
        
        ProductId productId = ProductId.of(request.getProductId());
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + request.getProductId()));
        
        List<CategoryId> categoryIds = request.getCategoryIds().stream()
                .map(CategoryId::of)
                .collect(Collectors.toList());
        
        if (!categoryIds.isEmpty()) {
            validateCategories(categoryIds);
        }
        
        product.assignCategories(categoryIds);
        
        Product savedProduct = productRepository.save(product);
        
        return AssignProductToCategoryResponse.success(savedProduct);
    }
    
    private void validateRequest(AssignProductToCategoryRequest request) {
        if (request.getCategoryIds() != null && request.getCategoryIds().size() > 5) {
            throw new MaxCategoryLimitException("상품은 최대 5개의 카테고리에만 할당할 수 있습니다");
        }
    }
    
    private void validateCategories(List<CategoryId> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        
        if (categories.size() != categoryIds.size()) {
            throw new InvalidCategoryIdException("Some categories not found");
        }
    }
}