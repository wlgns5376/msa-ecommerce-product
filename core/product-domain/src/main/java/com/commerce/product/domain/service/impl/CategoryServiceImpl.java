package com.commerce.product.domain.service.impl;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.repository.CategoryRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.CategoryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CategoryService 구현체
 */
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    
    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }
    
    @Override
    public CategoryTree buildCategoryTree() {
        // 모든 카테고리를 한 번에 조회하여 N+1 문제 해결
        List<Category> allCategories = categoryRepository.findAll();
        
        // 부모 ID별로 카테고리를 그룹화
        Map<CategoryId, List<Category>> categoryByParentId = new HashMap<>();
        List<Category> rootCategories = new ArrayList<>();
        
        for (Category category : allCategories) {
            if (category.getParentId() == null) {
                rootCategories.add(category);
            } else {
                categoryByParentId.computeIfAbsent(category.getParentId(), k -> new ArrayList<>())
                        .add(category);
            }
        }
        
        // 루트 카테고리부터 트리 구성
        List<CategoryNode> rootNodes = rootCategories.stream()
                .map(category -> buildCategoryNode(category, categoryByParentId))
                .collect(Collectors.toList());
        
        return new CategoryTree(rootNodes);
    }
    
    private CategoryNode buildCategoryNode(Category category, Map<CategoryId, List<Category>> categoryByParentId) {
        List<Category> children = categoryByParentId.getOrDefault(category.getId(), new ArrayList<>());
        List<CategoryNode> childNodes = children.stream()
                .map(child -> buildCategoryNode(child, categoryByParentId))
                .collect(Collectors.toList());
        
        return new CategoryNode(category, childNodes);
    }
    
    @Override
    public List<Category> getProductCategories(ProductId productId) {
        return productRepository.findById(productId)
                .map(product -> {
                    List<CategoryId> categoryIds = product.getCategoryIds();
                    // 한 번의 쿼리로 모든 카테고리 조회
                    return categoryRepository.findAllById(categoryIds);
                })
                .orElse(new ArrayList<>());
    }
    
    @Override
    public void assignProductToCategories(ProductId productId, List<CategoryId> categoryIds) {
        productRepository.findById(productId)
                .ifPresent(product -> {
                    product.assignCategories(categoryIds);
                    productRepository.save(product);
                });
    }
    
    @Override
    public List<Category> getCategoryPath(CategoryId categoryId) {
        return categoryRepository.findCategoryPath(categoryId);
    }
}