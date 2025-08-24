package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchProductsService implements SearchProductsUseCase {
    
    private final ProductRepository productRepository;
    
    @Override
    public SearchProductsResponse searchProducts(SearchProductsQuery query) {
        query.validate();
        
        String keyword = query.getKeyword().trim();
        int offset = query.getPage() * query.getSize();
        int limit = query.getSize();
        
        List<Product> products;
        long totalElements;
        
        if (query.isOnlyActive()) {
            products = productRepository.searchActiveByName(keyword, offset, limit);
            totalElements = productRepository.countActiveByName(keyword);
        } else {
            products = productRepository.searchByName(keyword, offset, limit);
            totalElements = productRepository.countByName(keyword);
        }
        
        return SearchProductsResponse.of(products, query.getPage(), query.getSize(), totalElements);
    }
}