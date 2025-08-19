package com.commerce.product.domain.application.usecase;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductName;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductService implements CreateProductUseCase {
    
    private final ProductRepository productRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public CreateProductResponse createProduct(CreateProductRequest request) {
        String description = Optional.ofNullable(request.getDescription()).orElse("");
        
        Product product = Product.create(
                new ProductName(request.getName()),
                description,
                request.getType()
        );
        
        productRepository.save(product);
        
        final var eventsToPublish = new ArrayList<>(product.getDomainEvents());
        product.clearDomainEvents();
        
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventsToPublish.forEach(eventPublisher::publish);
                }
            });
        } else {
            eventsToPublish.forEach(eventPublisher::publish);
        }
        
        return CreateProductResponse.builder()
                .productId(product.getId().toString())
                .name(product.getName().value())
                .description(product.getDescription())
                .type(product.getType())
                .status(product.getStatus())
                .build();
    }
}