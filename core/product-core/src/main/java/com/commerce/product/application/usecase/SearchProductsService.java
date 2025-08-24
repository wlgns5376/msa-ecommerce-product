package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.PagedResult;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
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
        ProductStatus status = query.isOnlyActive() ? ProductStatus.ACTIVE : null;
        PagedResult<Product> productPage = productRepository.search(keyword, query.getPage(), query.getSize(), status);
        
        return SearchProductsResponse.of(
            productPage.getContent(), 
            productPage.getPage(), 
            productPage.getSize(), 
            productPage.getTotalElements()
        );
    }
}