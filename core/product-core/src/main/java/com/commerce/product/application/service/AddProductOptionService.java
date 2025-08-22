package com.commerce.product.application.service;

import org.springframework.context.ApplicationEventPublisher;
import com.commerce.product.application.usecase.AddProductOptionRequest;
import com.commerce.product.application.usecase.AddProductOptionResponse;
import com.commerce.product.application.usecase.AddProductOptionUseCase;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.exception.InvalidProductOptionException;
import com.commerce.product.domain.exception.InvalidSkuMappingException;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddProductOptionService implements AddProductOptionUseCase {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AddProductOptionResponse addProductOption(AddProductOptionRequest request) {
        ProductId productId = new ProductId(request.getProductId());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new InvalidProductException("Product not found"));

        ProductOption option = createProductOption(request);

        product.addOption(option);
        productRepository.save(product);

        product.getDomainEvents().forEach(eventPublisher::publishEvent);

        return AddProductOptionResponse.builder()
            .productId(product.getId().value())
            .optionId(option.getId())
            .optionName(option.getName())
            .build();
    }

    private ProductOption createProductOption(AddProductOptionRequest request) {
        Currency currency;
        try {
            currency = Currency.valueOf(request.getCurrency());
        } catch (IllegalArgumentException e) {
            throw new InvalidProductOptionException("Invalid currency: " + request.getCurrency());
        }
        Money price = new Money(request.getPrice(), currency);
        SkuMapping skuMapping = SkuMapping.of(request.getSkuMappings());

        if (skuMapping.isBundle()) {
            return ProductOption.bundle(request.getOptionName(), price, skuMapping);
        } else {
            return ProductOption.single(request.getOptionName(), price, skuMapping);
        }
    }

}