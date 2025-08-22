package com.commerce.product.application.service;

import com.commerce.product.application.usecase.UpdateProductRequest;
import com.commerce.product.application.usecase.UpdateProductResponse;
import com.commerce.product.application.usecase.UpdateProductUseCase;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.model.ProductName;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProductService implements UpdateProductUseCase {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public UpdateProductResponse updateProduct(UpdateProductRequest request) {
        ProductId productId = new ProductId(request.getProductId());
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new InvalidProductException("Product not found with id: " + request.getProductId()));

        ProductName updatedName = request.getName() != null 
            ? new ProductName(request.getName()) 
            : product.getName();
            
        String updatedDescription = request.getDescription() != null 
            ? request.getDescription() 
            : product.getDescription();

        Product savedProduct = product;
        if (product.update(updatedName, updatedDescription)) {
            savedProduct = productRepository.save(product);
        }

        return UpdateProductResponse.builder()
            .productId(savedProduct.getId().value())
            .name(savedProduct.getName().value())
            .description(savedProduct.getDescription())
            .type(savedProduct.getType().name())
            .status(savedProduct.getStatus().name())
            .build();
    }
}