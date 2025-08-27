package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.ProductSearchCriteria;
import com.commerce.product.test.helper.ProductTestBuilder;
import com.commerce.product.test.helper.ProductSearchResultTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchProductsUseCase 테스트")
class SearchProductsUseCaseTest {
    
    @Mock
    private ProductRepository productRepository;
    
    private SearchProductsUseCase searchProductsUseCase;
    
    @BeforeEach
    void setUp() {
        searchProductsUseCase = new SearchProductsService(productRepository);
    }
    
    @Nested
    @DisplayName("상품 검색 시")
    class SearchProductsTest {
        
        @Test
        @DisplayName("키워드로 검색하면 이름이 매칭되는 상품 목록을 반환한다")
        void should_return_products_matching_keyword() {
            // Given
            String keyword = "티셔츠";
            SearchProductsRequest request = SearchProductsRequest.builder()
                .keyword(keyword)
                .page(0)
                .size(10)
                .build();
            
            List<ProductSearchResult> searchResults = createProductSearchResultsWithKeyword(keyword);
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 10), 2);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(2);
            assertThat(response.getProducts())
                .extracting(SearchProductsResponse.SearchProductItem::getName)
                .allMatch(name -> name.contains(keyword));
            assertThat(response.getPageInfo().getTotalElements()).isEqualTo(2);
            
            verify(productRepository).searchProductsOptimized(
                argThat(criteria -> keyword.equals(criteria.getKeyword())),
                any(Pageable.class)
            );
        }
        
        @Test
        @DisplayName("카테고리ID로 검색하면 해당 카테고리의 상품 목록을 반환한다")
        void should_return_products_in_category() {
            // Given
            String categoryId = "category-001";
            SearchProductsRequest request = SearchProductsRequest.builder()
                .categoryId(categoryId)
                .page(0)
                .size(20)
                .build();
            
            List<ProductSearchResult> searchResults = createProductSearchResultsInCategory(categoryId);
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 20), 3);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
            verify(productRepository).searchProductsOptimized(
                argThat(criteria -> categoryId.equals(criteria.getCategoryId())),
                any(Pageable.class)
            );
        }
        
        @Test
        @DisplayName("가격 범위로 검색하면 옵션 가격이 범위 내인 상품 목록을 반환한다")
        void should_return_products_within_price_range() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .minPrice(new BigDecimal("10000"))
                .maxPrice(new BigDecimal("50000"))
                .page(0)
                .size(10)
                .build();
            
            List<ProductSearchResult> searchResults = createProductSearchResultsInPriceRange();
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 10), 2);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(2);
            // 참고: 실제 필터링은 repository에서 수행되므로, 
            // 여기서는 repository가 올바른 criteria를 받았는지만 검증
            verify(productRepository).searchProductsOptimized(
                argThat(criteria -> 
                    criteria.getMinPrice() != null && criteria.getMinPrice().equals(new BigDecimal("10000")) &&
                    criteria.getMaxPrice() != null && criteria.getMaxPrice().equals(new BigDecimal("50000"))),
                any(Pageable.class));
        }
        
        @Test
        @DisplayName("상태 필터로 검색하면 해당 상태의 상품만 반환한다")
        void should_return_products_with_specific_status() {
            // Given
            Set<ProductStatus> statuses = Set.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE);
            SearchProductsRequest request = SearchProductsRequest.builder()
                .statuses(statuses)
                .page(0)
                .size(10)
                .build();
            
            List<ProductSearchResult> searchResults = createProductSearchResultsWithStatus(ProductStatus.ACTIVE);
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 10), 2);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(2);
            verify(productRepository).searchProductsOptimized(
                argThat(criteria -> criteria.getStatuses().containsAll(statuses)),
                any(Pageable.class)
            );
        }
        
        @Test
        @DisplayName("복합 조건으로 검색하면 모든 조건에 맞는 상품을 반환한다")
        void should_return_products_matching_all_criteria() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .categoryId("category-001")
                .keyword("프리미엄")
                .minPrice(new BigDecimal("30000"))
                .maxPrice(new BigDecimal("100000"))
                .statuses(Set.of(ProductStatus.ACTIVE))
                .page(0)
                .size(10)
                .sortBy("price")
                .sortDirection("ASC")
                .build();
            
            List<ProductSearchResult> searchResults = createPremiumProductSearchResults();
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 10), 1);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(1);
            assertThat(response.getProducts().get(0).getName()).contains("프리미엄");
            assertThat(response.getProducts().get(0).getMinPrice()).isGreaterThanOrEqualTo(new BigDecimal("30000"));
        }
        
        @Test
        @DisplayName("정렬 조건을 지정하면 해당 순서로 정렬된 결과를 반환한다")
        void should_return_sorted_products() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("name")
                .sortDirection("ASC")
                .build();
            
            List<ProductSearchResult> searchResults = createSortedProductSearchResults();
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 10, 
                Sort.by(Sort.Direction.ASC, "name")), 3);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
            assertThat(response.getProducts())
                .extracting(SearchProductsResponse.SearchProductItem::getName)
                .containsExactly("A상품", "B상품", "C상품");
            verify(productRepository).searchProductsOptimized(
                any(ProductSearchCriteria.class),
                argThat(pageable -> {
                    Sort.Order order = pageable.getSort().getOrderFor("name");
                    return order != null && order.getDirection() == Sort.Direction.ASC;
                })
            );
        }
        
        @Test
        @DisplayName("페이지네이션을 적용하면 요청한 페이지의 결과를 반환한다")
        void should_return_paginated_results() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(1)  // 두 번째 페이지
                .size(2)  // 페이지당 2개
                .build();
            
            List<ProductSearchResult> searchResults = createProductSearchResultsForPagination();
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(1, 2), 5);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(2);
            assertThat(response.getPageInfo().getCurrentPage()).isEqualTo(1);
            assertThat(response.getPageInfo().getPageSize()).isEqualTo(2);
            assertThat(response.getPageInfo().getTotalElements()).isEqualTo(5);
            assertThat(response.getPageInfo().getTotalPages()).isEqualTo(3);
            assertThat(response.getPageInfo().hasNext()).isTrue();
        }
        
        @Test
        @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
        void should_return_empty_list_when_no_results() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .keyword("존재하지않는상품")
                .build();
            
            Page<ProductSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(emptyPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).isEmpty();
            assertThat(response.getPageInfo().getTotalElements()).isZero();
            assertThat(response.getPageInfo().hasNext()).isFalse();
        }
        
        @Test
        @DisplayName("재고 상태는 상품 조회와 별개로 처리되어 검색에 영향을 주지 않는다")
        void should_not_filter_by_stock_availability() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(0)
                .size(10)
                .build();
            
            List<ProductSearchResult> searchResults = createProductSearchResultsWithMixedStock();
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 10), 3);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
        }
        
        @Test
        @DisplayName("상품의 최소/최대 가격을 옵션에서 계산하여 반환한다")
        void should_calculate_min_max_prices_from_options() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(0)
                .size(10)
                .build();
            
            ProductSearchResult productWithMultipleOptions = createProductSearchResultWithPriceRange();
            Page<ProductSearchResult> resultPage = new PageImpl<>(List.of(productWithMultipleOptions), 
                PageRequest.of(0, 10), 1);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(1);
            SearchProductsResponse.SearchProductItem item = response.getProducts().get(0);
            assertThat(item.getMinPrice()).isEqualTo(new BigDecimal("10000"));  // 최저가 옵션
            assertThat(item.getMaxPrice()).isEqualTo(new BigDecimal("30000"));  // 최고가 옵션
        }
        
        @Test
        @DisplayName("가격 범위로 검색 시 옵션 중 하나라도 범위에 포함되면 상품을 반환한다")
        void should_return_product_when_any_option_price_in_range() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .minPrice(new BigDecimal("15000"))
                .maxPrice(new BigDecimal("25000"))
                .page(0)
                .size(10)
                .build();
            
            // 상품의 옵션이 10000, 20000, 30000원인 경우
            // 20000원 옵션이 검색 범위에 포함되므로 이 상품이 반환되어야 함
            ProductSearchResult productWithMultipleOptions = createProductSearchResultWithPriceRange();
            Page<ProductSearchResult> resultPage = new PageImpl<>(List.of(productWithMultipleOptions), 
                PageRequest.of(0, 10), 1);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(1);
            SearchProductsResponse.SearchProductItem item = response.getProducts().get(0);
            
            // 응답의 minPrice/maxPrice는 상품의 전체 옵션 중 최소/최대값
            assertThat(item.getMinPrice()).isEqualTo(new BigDecimal("10000"));
            assertThat(item.getMaxPrice()).isEqualTo(new BigDecimal("30000"));
            
            // 상품이 검색된 이유: 20000원 옵션이 15000-25000 범위에 포함됨
            // 이는 repository의 searchProducts 쿼리가 올바르게 작동한다고 가정
        }
        
        @Test
        @DisplayName("옵션이 없는 상품의 경우 최소/최대 가격이 null을 반환한다")
        void should_return_null_prices_when_no_options() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(0)
                .size(10)
                .build();
            
            ProductSearchResult productWithoutOptions = ProductSearchResultTestBuilder.aProductSearchResult()
                .withId(UUID.randomUUID().toString())
                .withName("옵션 없는 상품")
                .withType(ProductType.NORMAL)
                .withStatus(ProductStatus.ACTIVE)
                .withMinPrice(null)  // 옵션이 없으므로 null
                .withMaxPrice(null)  // 옵션이 없으므로 null
                .build();
            Page<ProductSearchResult> resultPage = new PageImpl<>(List.of(productWithoutOptions), 
                PageRequest.of(0, 10), 1);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(1);
            SearchProductsResponse.SearchProductItem item = response.getProducts().get(0);
            assertThat(item.getMinPrice()).isNull();
            assertThat(item.getMaxPrice()).isNull();
        }
        
        @Test
        @DisplayName("허용되지 않은 정렬 필드를 요청하면 기본값(createdAt)으로 정렬한다")
        void should_use_default_sort_when_invalid_sort_field() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("invalidField")  // 허용되지 않은 정렬 필드
                .sortDirection("DESC")
                .build();
            
            List<ProductSearchResult> searchResults = createProductSearchResultsInCategory("CAT001");
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 10), 3);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
            verify(productRepository).searchProductsOptimized(
                any(ProductSearchCriteria.class),
                argThat(pageable -> {
                    Sort.Order order = pageable.getSort().getOrderFor("createdAt");
                    return order != null && order.getDirection() == Sort.Direction.DESC;
                })
            );
        }
        
        @Test
        @DisplayName("허용된 정렬 필드(name)로 요청하면 해당 필드로 정렬한다")
        void should_sort_by_allowed_fields() {
            // Given - name으로 정렬
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("name")
                .sortDirection("ASC")
                .build();
            
            List<ProductSearchResult> searchResults = createProductSearchResultsInCategory("CAT001");
            Page<ProductSearchResult> resultPage = new PageImpl<>(searchResults, PageRequest.of(0, 10), 3);
            
            given(productRepository.searchProductsOptimized(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(resultPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
            verify(productRepository).searchProductsOptimized(
                any(ProductSearchCriteria.class),
                argThat(pageable -> {
                    Sort.Order order = pageable.getSort().getOrderFor("name");
                    return order != null && order.getDirection() == Sort.Direction.ASC;
                })
            );
        }
    }
    
    // Helper methods for creating test data
    
    private List<ProductSearchResult> createProductSearchResultsWithKeyword(String keyword) {
        ProductSearchResult result1 = ProductSearchResultTestBuilder.aProductSearchResult()
            .withId(UUID.randomUUID().toString())
            .withName(keyword + " 기본형")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withMinPrice(new BigDecimal("20000"))
            .withMaxPrice(new BigDecimal("20000"))
            .build();
            
        ProductSearchResult result2 = ProductSearchResultTestBuilder.aProductSearchResult()
            .withId(UUID.randomUUID().toString())
            .withName("프리미엄 " + keyword)
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withMinPrice(new BigDecimal("50000"))
            .withMaxPrice(new BigDecimal("50000"))
            .build();
            
        return List.of(result1, result2);
    }
    
    private List<ProductSearchResult> createProductSearchResultsInCategory(String categoryId) {
        return List.of(
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "상품1", List.of(categoryId)),
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "상품2", List.of(categoryId)),
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "상품3", List.of(categoryId))
        );
    }
    
    private List<ProductSearchResult> createProductSearchResultsInPriceRange() {
        ProductSearchResult result1 = ProductSearchResultTestBuilder.aProductSearchResult()
            .withId(UUID.randomUUID().toString())
            .withName("저가 상품")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withMinPrice(new BigDecimal("15000"))
            .withMaxPrice(new BigDecimal("15000"))
            .build();
            
        ProductSearchResult result2 = ProductSearchResultTestBuilder.aProductSearchResult()
            .withId(UUID.randomUUID().toString())
            .withName("중가 상품")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withMinPrice(new BigDecimal("35000"))
            .withMaxPrice(new BigDecimal("35000"))
            .build();
            
        return List.of(result1, result2);
    }
    
    private List<ProductSearchResult> createProductSearchResultsWithStatus(ProductStatus status) {
        return List.of(
            ProductSearchResultTestBuilder.aProductSearchResult()
                .withId(UUID.randomUUID().toString())
                .withName("활성 상품1")
                .withStatus(status)
                .withMinPrice(new BigDecimal("20000"))
                .withMaxPrice(new BigDecimal("20000"))
                .build(),
            ProductSearchResultTestBuilder.aProductSearchResult()
                .withId(UUID.randomUUID().toString())
                .withName("활성 상품2")
                .withStatus(status)
                .withMinPrice(new BigDecimal("30000"))
                .withMaxPrice(new BigDecimal("30000"))
                .build()
        );
    }
    
    private ProductSearchResult createSimpleProductSearchResult(String id, String name, List<String> categoryIds) {
        return ProductSearchResultTestBuilder.aProductSearchResult()
            .withId(UUID.randomUUID().toString())  // UUID 형식 사용
            .withName(name)
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withMinPrice(new BigDecimal("20000"))
            .withMaxPrice(new BigDecimal("20000"))
            .withCategoryIds(categoryIds)
            .build();
    }
    
    private List<ProductSearchResult> createPremiumProductSearchResults() {
        return List.of(
            ProductSearchResultTestBuilder.aProductSearchResult()
                .withId(UUID.randomUUID().toString())
                .withName("프리미엄 한정판")
                .withType(ProductType.NORMAL)
                .withStatus(ProductStatus.ACTIVE)
                .withMinPrice(new BigDecimal("80000"))
                .withMaxPrice(new BigDecimal("80000"))
                .build()
        );
    }
    
    private List<ProductSearchResult> createSortedProductSearchResults() {
        return List.of(
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "A상품", List.of()),
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "B상품", List.of()),
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "C상품", List.of())
        );
    }
    
    private List<ProductSearchResult> createProductSearchResultsForPagination() {
        return List.of(
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "상품3", List.of()),
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "상품4", List.of())
        );
    }
    
    private List<ProductSearchResult> createProductSearchResultsWithMixedStock() {
        return List.of(
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "재고있음", List.of()),
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "재고없음", List.of()),
            createSimpleProductSearchResult(UUID.randomUUID().toString(), "재고소량", List.of())
        );
    }
    
    private ProductSearchResult createProductSearchResultWithPriceRange() {
        return ProductSearchResultTestBuilder.aProductSearchResult()
            .withId(UUID.randomUUID().toString())
            .withName("다양한 가격 옵션 상품")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withMinPrice(new BigDecimal("10000"))  // 최저가 옵션
            .withMaxPrice(new BigDecimal("30000"))  // 최고가 옵션
            .build();
    }
    
    // Helper methods for creating test data
    
    private List<Product> createProductsWithKeyword(String keyword) {
        Product product1 = ProductTestBuilder.builder()
            .withId(ProductId.generate())
            .withName(keyword + " 기본형")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withOption(ProductOption.single("기본", Money.of(20000), "SKU001"))
            .build();
            
        Product product2 = ProductTestBuilder.builder()
            .withId(ProductId.generate())
            .withName("프리미엄 " + keyword)
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withOption(ProductOption.single("프리미엄", Money.of(50000), "SKU002"))
            .build();
            
        return List.of(product1, product2);
    }
    
    private List<Product> createProductsInCategory() {
        return List.of(
            createSimpleProduct("product-001", "상품1"),
            createSimpleProduct("product-002", "상품2"),
            createSimpleProduct("product-003", "상품3")
        );
    }
    
    private List<Product> createProductsInPriceRange() {
        Product product1 = ProductTestBuilder.builder()
            .withId(ProductId.generate())
            .withName("저가 상품")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withOption(ProductOption.single("기본", Money.of(15000), "SKU001"))
            .build();
            
        Product product2 = ProductTestBuilder.builder()
            .withId(ProductId.generate())
            .withName("중가 상품")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withOption(ProductOption.single("기본", Money.of(35000), "SKU002"))
            .build();
            
        return List.of(product1, product2);
    }
    
    private List<Product> createProductsWithStatus(ProductStatus status) {
        return List.of(
            ProductTestBuilder.builder()
                .withId(ProductId.generate())
                .withName("활성 상품1")
                .withStatus(status)
                .withOption(ProductOption.single("기본", Money.of(20000), "SKU001"))
                .build(),
            ProductTestBuilder.builder()
                .withId(ProductId.generate())
                .withName("활성 상품2")
                .withStatus(status)
                .withOption(ProductOption.single("기본", Money.of(30000), "SKU002"))
                .build()
        );
    }
    
    private List<Product> createPremiumProducts() {
        return List.of(
            ProductTestBuilder.builder()
                .withId(ProductId.generate())
                .withName("프리미엄 한정판")
                .withType(ProductType.NORMAL)
                .withStatus(ProductStatus.ACTIVE)
                .withOption(ProductOption.single("한정판", Money.of(80000), "SKU001"))
                .build()
        );
    }
    
    private List<Product> createSortedProducts() {
        return List.of(
            createSimpleProduct("product-001", "A상품"),
            createSimpleProduct("product-002", "B상품"),
            createSimpleProduct("product-003", "C상품")
        );
    }
    
    private List<Product> createProductsForPagination() {
        return List.of(
            createSimpleProduct("product-003", "상품3"),
            createSimpleProduct("product-004", "상품4")
        );
    }
    
    private List<Product> createProductsWithMixedStock() {
        return List.of(
            createSimpleProduct("product-001", "재고있음"),
            createSimpleProduct("product-002", "재고없음"),
            createSimpleProduct("product-003", "재고소량")
        );
    }
    
    private Product createProductWithPriceRange() {
        return ProductTestBuilder.builder()
            .withId(ProductId.generate())
            .withName("다양한 가격 옵션 상품")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withOptions(List.of(
                ProductOption.single("저가 옵션", Money.of(10000), "SKU001"),
                ProductOption.single("중가 옵션", Money.of(20000), "SKU002"),
                ProductOption.single("고가 옵션", Money.of(30000), "SKU003")
            ))
            .build();
    }
    
    private Product createSimpleProduct(String id, String name) {
        String uuid = UUID.randomUUID().toString();
        return ProductTestBuilder.builder()
            .withId(ProductId.of(uuid))
            .withName(name)
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withOption(ProductOption.single("기본", Money.of(20000), "SKU-" + id))
            .build();
    }
}