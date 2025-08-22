package com.commerce.product.application.service;

import com.commerce.product.application.usecase.UpdateProductRequest;
import com.commerce.product.application.usecase.UpdateProductResponse;
import com.commerce.product.application.usecase.UpdateProductUseCase;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.exception.ProductConflictException;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
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
        
        // 버전 검증
        if (request.getVersion() != null && !request.getVersion().equals(product.getVersion())) {
            throw new ProductConflictException(
                "Product has been modified by another user. Please refresh and try again."
            );
        }

        final Product resultProduct;
        try {
            if (product.update(request.getName(), request.getDescription())) {
                resultProduct = productRepository.save(product);
            } else {
                resultProduct = product;
            }
        } catch (OptimisticLockingFailureException e) {
            throw new ProductConflictException(
                "Product has been modified by another user. Please refresh and try again.", e
            );
        }

        return UpdateProductResponse.builder()
            .productId(resultProduct.getId().value())
            .name(resultProduct.getName().value())
            .description(resultProduct.getDescription())
            .type(resultProduct.getType().name())
            .status(resultProduct.getStatus().name())
            .version(resultProduct.getVersion())
            .build();
    }
}