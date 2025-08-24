package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.InvalidProductIdException;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class GetProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetProductService getProductService;

    private Product product;
    private ProductId productId;

    @BeforeEach
    void setUp() {
        productId = new ProductId("550e8400-e29b-41d4-a716-446655440000");
        product = Product.create(
            new ProductName("테스트 상품"),
            "테스트 상품 설명",
            ProductType.NORMAL
        );
        
        // 리플렉션을 사용하여 ID 설정 (테스트 목적)
        try {
            var idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, productId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("상품 ID로 상품을 조회할 수 있다")
    void getProductById() {
        // Given
        GetProductQuery query = new GetProductQuery(productId.toString());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // When
        GetProductResponse response = getProductService.getProduct(query);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(productId.toString());
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getDescription()).isEqualTo("테스트 상품 설명");
        assertThat(response.getType()).isEqualTo("NORMAL");
        assertThat(response.getStatus()).isEqualTo("DRAFT");

        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("상품 옵션이 포함된 상품을 조회할 수 있다")
    void getProductWithOptions() {
        // Given
        // 상품에 옵션 추가
        Money price = new Money(BigDecimal.valueOf(10000), Currency.KRW);
        ProductOption option = ProductOption.single("기본 옵션", price, "SKU001");
        product.addOption(option);

        GetProductQuery query = new GetProductQuery(productId.toString());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // When
        GetProductResponse response = getProductService.getProduct(query);

        // Then
        assertThat(response.getOptions()).hasSize(1);
        assertThat(response.getOptions().get(0).getId()).isNotNull();
        assertThat(response.getOptions().get(0).getName()).isEqualTo("기본 옵션");
        assertThat(response.getOptions().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(10000));
        assertThat(response.getOptions().get(0).getCurrency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 조회 시 예외가 발생한다")
    void getProductWithNonExistentId() {
        // Given
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440001";
        GetProductQuery query = new GetProductQuery(nonExistentId);
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> getProductService.getProduct(query))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessage("상품을 찾을 수 없습니다: " + nonExistentId);
    }

    @Test
    @DisplayName("잘못된 형식의 상품 ID로 조회 시 예외가 발생한다")
    void getProductWithInvalidId() {
        // Given
        String invalidId = "invalid-uuid";
        GetProductQuery query = new GetProductQuery(invalidId);

        // When & Then
        assertThatThrownBy(() -> getProductService.getProduct(query))
            .isInstanceOf(InvalidProductIdException.class);
    }

    @Test
    @DisplayName("null 상품 ID로 조회 시 예외가 발생한다")
    void getProductWithNullId() {
        // Given
        GetProductQuery query = new GetProductQuery(null);

        // When & Then
        assertThatThrownBy(() -> getProductService.getProduct(query))
            .isInstanceOf(InvalidProductIdException.class);
    }

    @Test
    @DisplayName("비활성 상태의 상품도 조회할 수 있다")
    void getInactiveProduct() {
        // Given
        product.deactivate();
        GetProductQuery query = new GetProductQuery(productId.toString());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // When
        GetProductResponse response = getProductService.getProduct(query);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("INACTIVE");
    }
}