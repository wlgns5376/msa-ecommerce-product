package com.commerce.product.application.usecase;

import org.springframework.context.ApplicationEventPublisher;
import com.commerce.product.domain.event.ProductCreatedEvent;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.exception.InvalidProductNameException;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<ProductCreatedEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private CreateProductUseCase createProductUseCase;

    @BeforeEach
    void setUp() {
        createProductUseCase = new CreateProductService(productRepository, eventPublisher);
    }

    private static Stream<Arguments> productCreationProvider() {
        return Stream.of(
                Arguments.of("Test Product", "Test Description", ProductType.NORMAL),
                Arguments.of("Bundle Product", "Bundle Description", ProductType.BUNDLE)
        );
    }

    @ParameterizedTest(name = "{index}: {2} 타입 상품 생성")
    @MethodSource("productCreationProvider")
    @DisplayName("정상적인 상품 생성 요청 처리")
    void shouldCreateProductSuccessfully(String name, String description, ProductType type) {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name(name)
                .description(description)
                .type(type)
                .build();

        CreateProductResponse expectedResponse = CreateProductResponse.builder()
                .name(name)
                .description(description)
                .type(type)
                .status(ProductStatus.DRAFT)
                .build();

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CreateProductResponse response = createProductUseCase.createProduct(request);

        // Then
        assertThat(response.getProductId()).isNotNull();
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("productId")
                .isEqualTo(expectedResponse);

        verify(productRepository, times(1)).save(productCaptor.capture());
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getName().value()).isEqualTo(name);
        assertThat(savedProduct.getDescription()).isEqualTo(description);
        assertThat(savedProduct.getType()).isEqualTo(type);
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.DRAFT);

        ProductCreatedEvent event = eventCaptor.getValue();
        assertThat(event.getProductId()).isEqualTo(savedProduct.getId());
        assertThat(event.getName()).isEqualTo(name);
        assertThat(event.getType()).isEqualTo(type);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("이름이 null 또는 빈 문자열인 경우 예외 발생")
    void shouldThrowExceptionWhenNameIsNullOrEmpty(String name) {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name(name)
                .description("Test Description")
                .type(ProductType.NORMAL)
                .build();

        // When & Then
        assertThatThrownBy(() -> createProductUseCase.createProduct(request))
                .isInstanceOf(InvalidProductNameException.class)
                .hasMessageContaining("Product name cannot be null or empty");

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("타입이 null인 경우 예외 발생")
    void shouldThrowExceptionWhenTypeIsNull() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .type(null)
                .build();

        // When & Then
        assertThatThrownBy(() -> createProductUseCase.createProduct(request))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("상품 타입은 필수입니다");

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("설명이 null인 경우 빈 문자열로 처리")
    void shouldHandleNullDescription() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .description(null)
                .type(ProductType.NORMAL)
                .build();

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CreateProductResponse response = createProductUseCase.createProduct(request);

        // Then
        assertThat(response.getDescription()).isEmpty();
        verify(productRepository, times(1)).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getDescription()).isEmpty();
        verify(eventPublisher, times(1)).publishEvent(any(ProductCreatedEvent.class));
    }

    @Test
    @DisplayName("저장 실패 시 예외가 전파됨")
    void shouldPropagateExceptionWhenSaveFails() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .type(ProductType.NORMAL)
                .build();

        doThrow(new RuntimeException("Database error"))
                .when(productRepository).save(any());

        // When & Then
        assertThatThrownBy(() -> createProductUseCase.createProduct(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(eventPublisher, never()).publishEvent(any());
    }
}