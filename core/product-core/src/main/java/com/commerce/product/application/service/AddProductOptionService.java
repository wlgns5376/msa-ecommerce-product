package com.commerce.product.application.service;

import com.commerce.product.application.factory.ProductOptionFactory;
import com.commerce.product.application.service.port.out.EventPublisher;
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
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public AddProductOptionResponse addProductOption(AddProductOptionRequest request) {
        ProductId productId = new ProductId(request.getProductId());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new InvalidProductException("상품을 찾을 수 없습니다"));

        ProductOption option = productOptionFactory.create(request);

        product.addOption(option);
        productRepository.save(product);
        // 도메인 이벤트 발행
        eventPublisher.publishAll(product.pullDomainEvents());

        return AddProductOptionResponse.builder()
            .productId(product.getId().value())
            .optionId(option.getId())
            .optionName(option.getName())
            .build();
    }


}