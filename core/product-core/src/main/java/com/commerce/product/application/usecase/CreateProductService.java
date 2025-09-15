package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductName;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductService implements CreateProductUseCase {
    
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CreateProductResponse createProduct(CreateProductRequest request) {
        Product product = Product.create(
                new ProductName(request.getName()),
                request.getDescription(),
                request.getType()
        );
        
        Product savedProduct = productRepository.save(product);
        
        publishDomainEvents(product);
        
        return CreateProductResponse.from(savedProduct);
    }
    
    private void publishDomainEvents(Product product) {
        product.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
