package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductService implements GetProductUseCase {
    
    private final ProductRepository productRepository;
    
    @Override
    public GetProductResponse getProduct(GetProductQuery query) {
        ProductId productId = new ProductId(query.getProductId());
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다: " + query.getProductId()));
            
        return GetProductResponse.from(product);
    }
}