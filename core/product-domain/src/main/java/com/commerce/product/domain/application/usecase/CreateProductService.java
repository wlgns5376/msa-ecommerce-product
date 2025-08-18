package com.commerce.product.domain.application.usecase;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductName;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductService implements CreateProductUseCase {
    
    private final ProductRepository productRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public CreateProductResponse createProduct(CreateProductRequest request) {
        validateRequest(request);
        
        String description = request.getDescription() != null ? request.getDescription() : "";
        
        Product product = Product.create(
                new ProductName(request.getName()),
                description,
                request.getType()
        );
        
        productRepository.save(product);
        
        product.getDomainEvents().forEach(eventPublisher::publish);
        product.clearDomainEvents();
        
        return CreateProductResponse.builder()
                .productId(product.getId().toString())
                .name(product.getName().value())
                .description(product.getDescription())
                .type(product.getType())
                .status(product.getStatus())
                .build();
    }
    
    private void validateRequest(CreateProductRequest request) {
        if (request.getName() == null) {
            throw new InvalidProductException("Product name is required");
        }
        if (request.getType() == null) {
            throw new InvalidProductException("Product type is required");
        }
    }
}