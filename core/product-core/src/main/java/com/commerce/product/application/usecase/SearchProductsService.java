package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.ProductSearchCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 상품 검색 유스케이스 구현
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SearchProductsService implements SearchProductsUseCase {
    
    private final ProductRepository productRepository;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "name", "price");
    
    public SearchProductsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @Override
    public SearchProductsResponse execute(SearchProductsRequest request) {
        // 검색 조건 생성
        ProductSearchCriteria criteria = buildSearchCriteria(request);
        
        // 페이징 정보 생성
        Pageable pageable = createPageable(request);
        
        // 상품 검색
        // TODO: 성능 개선 - 현재는 Product 엔티티를 조회한 후 각 옵션에 대해 가격을 계산하고 있음.
        // 대량의 상품 조회 시 N+1 문제가 발생할 수 있으므로, 추후 Repository에서 
        // 가격 정보를 포함한 DTO를 직접 조회하는 방식으로 개선 필요
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
        // 허용되지 않은 정렬 필드인 경우 기본값으로 설정
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "createdAt";
        }
        
        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection)
            .orElse(Sort.Direction.DESC);
        return Sort.by(direction, sortBy);
    }
    
    private SearchProductsResponse.SearchProductItem convertToSearchItem(Product product) {
        // 옵션에서 최소/최대 가격 계산
        Optional<BigDecimal> minPriceOpt = product.getOptions().stream()
                .map(option -> option.getPrice().amount())
                .min(Comparator.naturalOrder());
        
        Optional<BigDecimal> maxPriceOpt = product.getOptions().stream()
                .map(option -> option.getPrice().amount())
                .max(Comparator.naturalOrder());

        BigDecimal minPrice = minPriceOpt.orElse(null);
        BigDecimal maxPrice = maxPriceOpt.orElse(null);
        
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
}