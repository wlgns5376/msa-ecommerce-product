package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class SearchProductsResponse {
    private final List<ProductSearchResult> products;
    private final int page;
    private final int size;
    private final long totalElements;
    
    public static SearchProductsResponse of(List<Product> products, int page, int size, long totalElements) {
        List<ProductSearchResult> results = products.stream()
            .map(ProductSearchResult::from)
            .collect(Collectors.toList());
            
        return SearchProductsResponse.builder()
            .products(results)
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .build();
    }
    
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProductSearchResult {
        private final String id;
        private final String name;
        private final String description;
        private final String type;
        private final String status;
        
        public static ProductSearchResult from(Product product) {
            return ProductSearchResult.builder()
                .id(product.getId().toString())
                .name(product.getName().value())
                .description(product.getDescription())
                .type(product.getType().name())
                .status(product.getStatus().name())
                .build();
        }
    }
}