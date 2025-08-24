package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
        
        List<Product> products = productRepository.searchByName(keyword, offset, limit);
        
        if (query.isOnlyActive()) {
            products = products.stream()
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .collect(Collectors.toList());
        }
        
        return SearchProductsResponse.of(products, query.getPage(), query.getSize());
    }
}