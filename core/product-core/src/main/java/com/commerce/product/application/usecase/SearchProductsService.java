package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.ProductSearchCriteria;
import com.commerce.product.domain.service.StockAvailabilityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 검색 유스케이스 구현
 */
@Service
@Transactional(readOnly = true)
public class SearchProductsService implements SearchProductsUseCase {
    
    private final ProductRepository productRepository;
    private final StockAvailabilityService stockAvailabilityService;
    
    public SearchProductsService(ProductRepository productRepository, 
                                StockAvailabilityService stockAvailabilityService) {
        this.productRepository = productRepository;
        this.stockAvailabilityService = stockAvailabilityService;
    }
    
    @Override
    public SearchProductsResponse execute(SearchProductsRequest request) {
        // 검색 조건 생성
        ProductSearchCriteria criteria = buildSearchCriteria(request);
        
        // 페이징 정보 생성
        Pageable pageable = createPageable(request);
        
        // 상품 검색
        Page<Product> productPage = productRepository.searchProducts(criteria, pageable);
        
        // 응답 변환
        List<SearchProductsResponse.SearchProductItem> items = productPage.getContent().stream()
            .map(this::convertToSearchItem)
            .collect(Collectors.toList());
        
        // 페이지 정보 생성
        SearchProductsResponse.PageInfo pageInfo = new SearchProductsResponse.PageInfo(
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements()
        );
        
        return new SearchProductsResponse(items, pageInfo);
    }
    
    private ProductSearchCriteria buildSearchCriteria(SearchProductsRequest request) {
        return ProductSearchCriteria.builder()
            .categoryId(request.getCategoryId())
            .keyword(request.getKeyword())
            .minPrice(request.getMinPrice())
            .maxPrice(request.getMaxPrice())
            .statuses(request.getStatuses())
            .build();
    }
    
    private Pageable createPageable(SearchProductsRequest request) {
        Sort sort = createSort(request.getSortBy(), request.getSortDirection());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
    
    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }
    
    private SearchProductsResponse.SearchProductItem convertToSearchItem(Product product) {
        // 옵션에서 최소/최대 가격 계산
        Integer minPrice = calculateMinPrice(product.getOptions());
        Integer maxPrice = calculateMaxPrice(product.getOptions());
        
        // 카테고리 ID 목록 추출
        List<String> categoryIds = product.getCategoryIds().stream()
            .map(CategoryId::value)
            .collect(Collectors.toList());
        
        return SearchProductsResponse.SearchProductItem.builder()
            .productId(product.getId().value())
            .name(product.getName().value())
            .description(product.getDescription())
            .productType(product.getType().name())
            .status(product.getStatus().name())
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .isAvailable(!product.getOptions().isEmpty())  // 옵션이 있으면 구매 가능
            .categoryIds(categoryIds)
            .build();
    }
    
    private Integer calculateMinPrice(List<ProductOption> options) {
        if (options.isEmpty()) {
            return 0;
        }
        
        return options.stream()
            .map(option -> option.getPrice().amount())
            .map(bigDecimal -> bigDecimal.intValue())
            .min(Integer::compareTo)
            .orElse(0);
    }
    
    private Integer calculateMaxPrice(List<ProductOption> options) {
        if (options.isEmpty()) {
            return 0;
        }
        
        return options.stream()
            .map(option -> option.getPrice().amount())
            .map(bigDecimal -> bigDecimal.intValue())
            .max(Integer::compareTo)
            .orElse(0);
    }
}