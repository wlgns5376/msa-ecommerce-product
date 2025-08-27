package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.ProductSearchCriteria;
import com.commerce.product.test.helper.ProductTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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
            
            List<Product> products = createProductsWithKeyword(keyword);
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 2);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(2);
            assertThat(response.getProducts())
                .extracting(SearchProductsResponse.SearchProductItem::getName)
                .allMatch(name -> name.contains(keyword));
            assertThat(response.getPageInfo().getTotalElements()).isEqualTo(2);
            
            verify(productRepository).searchProducts(
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
            
            List<Product> products = createProductsInCategory();
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 3);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
            verify(productRepository).searchProducts(
                argThat(criteria -> categoryId.equals(criteria.getCategoryId())),
                any(Pageable.class)
            );
        }
        
        @Test
        @DisplayName("가격 범위로 검색하면 범위 내 상품 목록을 반환한다")
        void should_return_products_within_price_range() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .minPrice(10000)
                .maxPrice(50000)
                .page(0)
                .size(10)
                .build();
            
            List<Product> products = createProductsInPriceRange();
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 2);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(2);
            assertThat(response.getProducts())
                .allMatch(p -> p.getMinPrice() >= 10000 && p.getMaxPrice() <= 50000);
            verify(productRepository).searchProducts(
                argThat(criteria -> 
                    criteria.getMinPrice() != null && criteria.getMinPrice().equals(10000) &&
                    criteria.getMaxPrice() != null && criteria.getMaxPrice().equals(50000)),
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
            
            List<Product> products = createProductsWithStatus(ProductStatus.ACTIVE);
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 2);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(2);
            verify(productRepository).searchProducts(
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
                .minPrice(30000)
                .maxPrice(100000)
                .statuses(Set.of(ProductStatus.ACTIVE))
                .page(0)
                .size(10)
                .sortBy("price")
                .sortDirection("ASC")
                .build();
            
            List<Product> products = createPremiumProducts();
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(1);
            assertThat(response.getProducts().get(0).getName()).contains("프리미엄");
            assertThat(response.getProducts().get(0).getMinPrice()).isGreaterThanOrEqualTo(30000);
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
            
            List<Product> products = createSortedProducts();
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10, 
                Sort.by(Sort.Direction.ASC, "name")), 3);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
            assertThat(response.getProducts())
                .extracting(SearchProductsResponse.SearchProductItem::getName)
                .containsExactly("A상품", "B상품", "C상품");
            verify(productRepository).searchProducts(
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
            
            List<Product> products = createProductsForPagination();
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(1, 2), 5);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
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
            
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
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
            
            List<Product> products = createProductsWithMixedStock();
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 3);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
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
            
            Product productWithMultipleOptions = createProductWithPriceRange();
            Page<Product> productPage = new PageImpl<>(List.of(productWithMultipleOptions), 
                PageRequest.of(0, 10), 1);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(1);
            SearchProductsResponse.SearchProductItem item = response.getProducts().get(0);
            assertThat(item.getMinPrice()).isEqualTo(10000);  // 최저가 옵션
            assertThat(item.getMaxPrice()).isEqualTo(30000);  // 최고가 옵션
        }
        
        @Test
        @DisplayName("옵션이 없는 상품의 경우 최소/최대 가격이 null을 반환한다")
        void should_return_null_prices_when_no_options() {
            // Given
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(0)
                .size(10)
                .build();
            
            Product productWithoutOptions = ProductTestBuilder.builder()
                .withId(ProductId.generate())
                .withName("옵션 없는 상품")
                .withType(ProductType.NORMAL)
                .withStatus(ProductStatus.ACTIVE)
                .withOptions(List.of())  // 빈 옵션 리스트
                .build();
            Page<Product> productPage = new PageImpl<>(List.of(productWithoutOptions), 
                PageRequest.of(0, 10), 1);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
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
            
            List<Product> products = createProductsInCategory();
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 3);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
            verify(productRepository).searchProducts(
                any(ProductSearchCriteria.class),
                argThat(pageable -> {
                    Sort.Order order = pageable.getSort().getOrderFor("createdAt");
                    return order != null && order.getDirection() == Sort.Direction.DESC;
                })
            );
        }
        
        @Test
        @DisplayName("허용된 정렬 필드(name, price)로 요청하면 해당 필드로 정렬한다")
        void should_sort_by_allowed_fields() {
            // Given - price로 정렬
            SearchProductsRequest request = SearchProductsRequest.builder()
                .page(0)
                .size(10)
                .sortBy("price")
                .sortDirection("ASC")
                .build();
            
            List<Product> products = createProductsInCategory();
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 3);
            
            given(productRepository.searchProducts(any(ProductSearchCriteria.class), any(Pageable.class)))
                .willReturn(productPage);
            
            // When
            SearchProductsResponse response = searchProductsUseCase.execute(request);
            
            // Then
            assertThat(response.getProducts()).hasSize(3);
            verify(productRepository).searchProducts(
                any(ProductSearchCriteria.class),
                argThat(pageable -> {
                    Sort.Order order = pageable.getSort().getOrderFor("price");
                    return order != null && order.getDirection() == Sort.Direction.ASC;
                })
            );
        }
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