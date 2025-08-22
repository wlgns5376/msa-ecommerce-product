package com.commerce.product.application.service;

import com.commerce.product.application.factory.ProductOptionFactory;
import com.commerce.product.application.usecase.AddProductOptionRequest;
import com.commerce.product.application.usecase.AddProductOptionResponse;
import com.commerce.product.application.usecase.AddProductOptionUseCase;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.model.ProductOption;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddProductOptionService implements AddProductOptionUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionFactory productOptionFactory;

    @Override
    @Transactional
    public AddProductOptionResponse addProductOption(AddProductOptionRequest request) {
        ProductId productId = new ProductId(request.getProductId());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new InvalidProductException("Product not found"));

        ProductOption option = productOptionFactory.create(request);

        product.addOption(option);
        productRepository.save(product);

        return AddProductOptionResponse.builder()
            .productId(product.getId().value())
            .optionId(option.getId())
            .optionName(option.getName())
            .build();
    }


}