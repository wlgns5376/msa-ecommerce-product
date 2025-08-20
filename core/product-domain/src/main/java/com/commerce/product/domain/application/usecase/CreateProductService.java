package com.commerce.product.domain.application.usecase;

import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductName;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductService implements CreateProductUseCase {
    
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CreateProductResponse createProduct(CreateProductRequest request) {
        String description = Optional.ofNullable(request.getDescription()).orElse("");
        
        Product product = Product.create(
                new ProductName(request.getName()),
                description,
                request.getType()
        );
        
        productRepository.save(product);
        
        publishDomainEvents(product);
        
        return CreateProductResponse.builder()
                .productId(product.getId().toString())
                .name(product.getName().value())
                .description(product.getDescription())
                .type(product.getType())
                .status(product.getStatus())
                .build();
    }
    
    private void publishDomainEvents(Product product) {
        product.getDomainEvents().forEach(eventPublisher::publishEvent);
        product.clearDomainEvents();
    }
}
