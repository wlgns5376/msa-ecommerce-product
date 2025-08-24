package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchProductsService implements SearchProductsUseCase {
    
    private final ProductRepository productRepository;
    
    @Override
    public SearchProductsResponse searchProducts(SearchProductsQuery query) {
        query.validate();
        
        String keyword = query.getKeyword().trim();
        String status = query.isOnlyActive() ? com.commerce.product.domain.model.ProductStatus.ACTIVE.name() : null;
        Page<Product> productPage = productRepository.search(keyword, query.getPage(), query.getSize(), status);
        
        return SearchProductsResponse.of(
            productPage.getContent(), 
            productPage.getNumber(), 
            productPage.getSize(), 
            productPage.getTotalElements()
        );
    }
}