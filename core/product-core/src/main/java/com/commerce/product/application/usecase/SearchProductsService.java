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

import java.util.List;
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
        
        // 상품 검색 - 최적화된 버전 사용 (가격 정보가 이미 계산된 DTO 반환)
        Page<ProductSearchResult> searchResultPage = productRepository.searchProductsOptimized(criteria, pageable);
        
        // 응답 변환
        List<SearchProductsResponse.SearchProductItem> items = searchResultPage.getContent().stream()
            .map(this::convertSearchResultToItem)
            .toList();
        
        // 페이지 정보 생성
        SearchProductsResponse.PageInfo pageInfo = new SearchProductsResponse.PageInfo(
            searchResultPage.getNumber(),
            searchResultPage.getSize(),
            searchResultPage.getTotalElements()
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
    
    private SearchProductsResponse.SearchProductItem convertSearchResultToItem(ProductSearchResult result) {
        // 카테고리 ID 목록 추출
        List<String> categoryIds = result.categoryIds().stream()
            .map(CategoryId::value)
            .toList();
        
        return SearchProductsResponse.SearchProductItem.builder()
            .productId(result.id().value())
            .name(result.name().value())
            .description(result.description())
            .productType(result.type().name())
            .status(result.status().name())
            .minPrice(result.minPrice())
            .maxPrice(result.maxPrice())
            .isAvailable(result.status() == ProductStatus.ACTIVE && result.minPrice() != null)  // 상태가 ACTIVE이고 가격이 있을 때만 구매 가능
            .categoryIds(categoryIds)
            .build();
    }
}