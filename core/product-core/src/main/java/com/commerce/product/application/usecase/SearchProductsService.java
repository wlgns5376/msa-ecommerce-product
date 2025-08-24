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
        
        Page<Product> productPage;
        if (query.isOnlyActive()) {
            productPage = productRepository.searchActiveByName(keyword, query.getPage(), query.getSize());
        } else {
            productPage = productRepository.searchByName(keyword, query.getPage(), query.getSize());
        }
        
        return SearchProductsResponse.of(
            productPage.getContent(), 
            productPage.getNumber(), 
            productPage.getSize(), 
            productPage.getTotalElements()
        );
    }
}