package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SearchProductsRequest 단위 테스트")
class SearchProductsRequestTest {

    @Test
    @DisplayName("정상적인 파라미터로 요청을 생성하면 성공한다")
    void should_create_request_with_valid_parameters() {
        // When
        SearchProductsRequest request = SearchProductsRequest.builder()
            .categoryId("category-001")
            .keyword("검색어")
            .minPrice(1000)
            .maxPrice(50000)
            .statuses(Set.of(ProductStatus.ACTIVE))
            .page(0)
            .size(20)
            .sortBy("createdAt")
            .sortDirection("desc")
            .build();
        
        // Then
        assertThat(request.getCategoryId()).isEqualTo("category-001");
        assertThat(request.getKeyword()).isEqualTo("검색어");
        assertThat(request.getMinPrice()).isEqualTo(1000);
        assertThat(request.getMaxPrice()).isEqualTo(50000);
        assertThat(request.getStatuses()).containsExactly(ProductStatus.ACTIVE);
        assertThat(request.getPage()).isEqualTo(0);
        assertThat(request.getSize()).isEqualTo(20);
        assertThat(request.getSortBy()).isEqualTo("createdAt");
        assertThat(request.getSortDirection()).isEqualTo("desc");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1, -10, -100})
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다")
    void should_throw_exception_when_page_is_negative(int page) {
        // When & Then
        assertThatThrownBy(() -> SearchProductsRequest.builder()
                .page(page)
                .size(20)
                .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("페이지 번호는 0보다 작을 수 없습니다.");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("페이지 크기가 1보다 작으면 예외가 발생한다")
    void should_throw_exception_when_size_is_less_than_one(int size) {
        // When & Then
        assertThatThrownBy(() -> SearchProductsRequest.builder()
                .page(0)
                .size(size)
                .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("페이지 크기는 1보다 작을 수 없습니다.");
    }
    
    @Test
    @DisplayName("최소 가격이 최대 가격보다 크면 예외가 발생한다")
    void should_throw_exception_when_min_price_greater_than_max_price() {
        // When & Then
        assertThatThrownBy(() -> SearchProductsRequest.builder()
                .minPrice(10000)
                .maxPrice(5000)
                .page(0)
                .size(20)
                .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("최소 가격은 최대 가격보다 클 수 없습니다.");
    }
    
    @Test
    @DisplayName("상태 목록을 설정하지 않으면 기본값으로 설정된다")
    void should_use_default_statuses_when_not_provided() {
        // When
        SearchProductsRequest request = SearchProductsRequest.builder()
            .page(0)
            .size(20)
            .build();
        
        // Then
        assertThat(request.getStatuses()).containsExactly(ProductStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("상태 목록을 명시적으로 설정하면 해당 값이 사용된다")
    void should_use_provided_statuses() {
        // Given
        Set<ProductStatus> statuses = Set.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE);
        
        // When
        SearchProductsRequest request = SearchProductsRequest.builder()
            .statuses(statuses)
            .page(0)
            .size(20)
            .build();
        
        // Then
        assertThat(request.getStatuses()).containsExactlyInAnyOrder(ProductStatus.ACTIVE, ProductStatus.INACTIVE);
    }
    
    @Test
    @DisplayName("상태 목록을 null로 설정하면 기본값으로 설정된다")
    void should_use_default_statuses_when_explicitly_set_to_null() {
        // When
        SearchProductsRequest request = SearchProductsRequest.builder()
            .statuses(null)
            .page(0)
            .size(20)
            .build();
        
        // Then
        assertThat(request.getStatuses()).containsExactly(ProductStatus.ACTIVE);
    }
}