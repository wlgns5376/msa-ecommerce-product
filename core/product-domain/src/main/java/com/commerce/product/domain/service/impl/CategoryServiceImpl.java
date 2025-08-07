package com.commerce.product.domain.service.impl;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.repository.CategoryRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.CategoryService;

import java.util.ArrayList;
import java.util.List;
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
        List<Category> rootCategories = categoryRepository.findRootCategories();
        List<CategoryNode> rootNodes = rootCategories.stream()
                .map(this::buildCategoryNode)
                .collect(Collectors.toList());
        
        return new CategoryTree(rootNodes);
    }
    
    private CategoryNode buildCategoryNode(Category category) {
        List<Category> children = categoryRepository.findByParentId(category.getId());
        List<CategoryNode> childNodes = children.stream()
                .map(this::buildCategoryNode)
                .collect(Collectors.toList());
        
        return new CategoryNode(category, childNodes);
    }
    
    @Override
    public List<Category> getProductCategories(ProductId productId) {
        return productRepository.findById(productId)
                .map(product -> product.getCategoryIds().stream()
                        .map(categoryRepository::findById)
                        .filter(java.util.Optional::isPresent)
                        .map(java.util.Optional::get)
                        .collect(Collectors.toList()))
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