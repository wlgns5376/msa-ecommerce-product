package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SearchProductsUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SearchProductsService searchProductsService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = Product.create(
            new ProductName("테스트 상품1"),
            "테스트 상품1 설명",
            ProductType.NORMAL
        );
        
        product2 = Product.create(
            new ProductName("테스트 상품2"),
            "테스트 상품2 설명",
            ProductType.BUNDLE
        );
    }

    @Test
    @DisplayName("키워드로 상품을 검색할 수 있다")
    void searchProductsByKeyword() {
        // Given
        String keyword = "테스트";
        int page = 0;
        int size = 10;
        SearchProductsQuery query = SearchProductsQuery.builder()
            .keyword(keyword)
            .page(page)
            .size(size)
            .build();

        List<Product> products = Arrays.asList(product1, product2);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(page, size), 2);
        when(productRepository.searchByName(keyword, page, size))
            .thenReturn(productPage);

        // When
        SearchProductsResponse response = searchProductsService.searchProducts(query);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProducts()).hasSize(2);
        assertThat(response.getProducts().get(0).getName()).isEqualTo("테스트 상품1");
        assertThat(response.getProducts().get(1).getName()).isEqualTo("테스트 상품2");
        assertThat(response.getPage()).isEqualTo(page);
        assertThat(response.getSize()).isEqualTo(size);
        assertThat(response.getTotalElements()).isEqualTo(2);

        verify(productRepository, times(1)).searchByName(keyword, page, size);
    }

    @Test
    @DisplayName("검색 결과가 없을 때 빈 리스트를 반환한다")
    void searchProductsWithNoResults() {
        // Given
        String keyword = "존재하지않는상품";
        int page = 0;
        int size = 10;
        SearchProductsQuery query = SearchProductsQuery.builder()
            .keyword(keyword)
            .page(page)
            .size(size)
            .build();

        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        when(productRepository.searchByName(keyword, page, size))
            .thenReturn(emptyPage);

        // When
        SearchProductsResponse response = searchProductsService.searchProducts(query);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProducts()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지네이션을 적용하여 검색할 수 있다")
    void searchProductsWithPagination() {
        // Given
        String keyword = "테스트";
        int page = 1;
        int size = 5;
        SearchProductsQuery query = SearchProductsQuery.builder()
            .keyword(keyword)
            .page(page)
            .size(size)
            .build();

        List<Product> products = Arrays.asList(product1);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(page, size), 10);
        when(productRepository.searchByName(keyword, page, size))
            .thenReturn(productPage);

        // When
        SearchProductsResponse response = searchProductsService.searchProducts(query);

        // Then
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(page);
        assertThat(response.getSize()).isEqualTo(size);

        verify(productRepository, times(1)).searchByName(keyword, page, size);
    }

    @Test
    @DisplayName("키워드가 null이거나 빈 문자열일 때 예외가 발생한다")
    void searchProductsWithInvalidKeyword() {
        // Given - null keyword
        SearchProductsQuery queryWithNull = SearchProductsQuery.builder()
            .keyword(null)
            .page(0)
            .size(10)
            .build();

        // When & Then
        assertThatThrownBy(() -> searchProductsService.searchProducts(queryWithNull))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("검색 키워드는 필수입니다");

        // Given - empty keyword
        SearchProductsQuery queryWithEmpty = SearchProductsQuery.builder()
            .keyword("")
            .page(0)
            .size(10)
            .build();

        // When & Then
        assertThatThrownBy(() -> searchProductsService.searchProducts(queryWithEmpty))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("검색 키워드는 필수입니다");

        // Given - blank keyword
        SearchProductsQuery queryWithBlank = SearchProductsQuery.builder()
            .keyword("   ")
            .page(0)
            .size(10)
            .build();

        // When & Then
        assertThatThrownBy(() -> searchProductsService.searchProducts(queryWithBlank))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("검색 키워드는 필수입니다");
    }

    @Test
    @DisplayName("페이지 번호가 음수일 때 예외가 발생한다")
    void searchProductsWithNegativePage() {
        // Given
        SearchProductsQuery query = SearchProductsQuery.builder()
            .keyword("테스트")
            .page(-1)
            .size(10)
            .build();

        // When & Then
        assertThatThrownBy(() -> searchProductsService.searchProducts(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("페이지 번호는 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("페이지 크기가 0 이하일 때 예외가 발생한다")
    void searchProductsWithInvalidSize() {
        // Given
        SearchProductsQuery query = SearchProductsQuery.builder()
            .keyword("테스트")
            .page(0)
            .size(0)
            .build();

        // When & Then
        assertThatThrownBy(() -> searchProductsService.searchProducts(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("페이지 크기는 1에서 100 사이여야 합니다");
    }

    @Test
    @DisplayName("페이지 크기가 100을 초과할 때 예외가 발생한다")
    void searchProductsWithSizeExceedsMaximum() {
        // Given
        SearchProductsQuery query = SearchProductsQuery.builder()
            .keyword("테스트")
            .page(0)
            .size(101)
            .build();

        // When & Then
        assertThatThrownBy(() -> searchProductsService.searchProducts(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("페이지 크기는 1에서 100 사이여야 합니다");
    }

    @Test
    @DisplayName("활성 상태의 상품만 검색 결과에 포함된다")
    void searchOnlyActiveProducts() {
        // Given
        String keyword = "테스트";
        SearchProductsQuery query = SearchProductsQuery.builder()
            .keyword(keyword)
            .page(0)
            .size(10)
            .onlyActive(true)
            .build();

        // product1에 옵션을 추가하고 활성 상태로 설정
        ProductOption option = ProductOption.single(
            "기본 옵션",
            new Money(java.math.BigDecimal.valueOf(10000), Currency.KRW),
            "test-sku-id"
        );
        product1.addOption(option);
        product1.activate();
        
        List<Product> activeProducts = Arrays.asList(product1);
        Page<Product> activePage = new PageImpl<>(activeProducts, PageRequest.of(0, 10), 1);
        when(productRepository.searchActiveByName(keyword, 0, 10))
            .thenReturn(activePage);

        // When
        SearchProductsResponse response = searchProductsService.searchProducts(query);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getName()).isEqualTo("테스트 상품1");
        assertThat(response.getProducts().get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getTotalElements()).isEqualTo(1);
        
        verify(productRepository, times(1)).searchActiveByName(keyword, 0, 10);
    }
}