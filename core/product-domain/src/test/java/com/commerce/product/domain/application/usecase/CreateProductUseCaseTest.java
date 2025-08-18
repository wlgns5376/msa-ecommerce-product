package com.commerce.product.domain.application.usecase;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.event.ProductCreatedEvent;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.exception.InvalidProductNameException;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import com.commerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<ProductCreatedEvent> eventCaptor;

    private CreateProductUseCase createProductUseCase;

    @BeforeEach
    void setUp() {
        createProductUseCase = new CreateProductService(productRepository, eventPublisher);
    }

    @Test
    @DisplayName("정상적인 상품 생성 요청 처리")
    void shouldCreateProductSuccessfully() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .type(ProductType.NORMAL)
                .build();

        // When
        CreateProductResponse response = createProductUseCase.createProduct(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getDescription()).isEqualTo("Test Description");
        assertThat(response.getType()).isEqualTo(ProductType.NORMAL);
        assertThat(response.getStatus()).isEqualTo(ProductStatus.DRAFT);

        verify(productRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publish(eventCaptor.capture());

        ProductCreatedEvent event = eventCaptor.getValue();
        assertThat(event.getName()).isEqualTo("Test Product");
        assertThat(event.getType()).isEqualTo(ProductType.NORMAL);
    }

    @Test
    @DisplayName("Bundle 타입 상품 생성")
    void shouldCreateBundleProductSuccessfully() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Bundle Product")
                .description("Bundle Description")
                .type(ProductType.BUNDLE)
                .build();

        // When
        CreateProductResponse response = createProductUseCase.createProduct(request);

        // Then
        assertThat(response.getType()).isEqualTo(ProductType.BUNDLE);
        assertThat(response.getStatus()).isEqualTo(ProductStatus.DRAFT);
        
        verify(productRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publish(any(ProductCreatedEvent.class));
    }

    @Test
    @DisplayName("이름이 null인 경우 예외 발생")
    void shouldThrowExceptionWhenNameIsNull() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name(null)
                .description("Test Description")
                .type(ProductType.NORMAL)
                .build();

        // When & Then
        assertThatThrownBy(() -> createProductUseCase.createProduct(request))
                .isInstanceOf(InvalidProductNameException.class)
                .hasMessageContaining("Product name cannot be null or empty");

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("이름이 빈 문자열인 경우 예외 발생")
    void shouldThrowExceptionWhenNameIsEmpty() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("")
                .description("Test Description")
                .type(ProductType.NORMAL)
                .build();

        // When & Then
        assertThatThrownBy(() -> createProductUseCase.createProduct(request))
                .isInstanceOf(InvalidProductNameException.class)
                .hasMessageContaining("Product name cannot be null or empty");

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
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
                .hasMessageContaining("Product type is required");

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
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

        // When
        CreateProductResponse response = createProductUseCase.createProduct(request);

        // Then
        assertThat(response.getDescription()).isEmpty();
        verify(productRepository, times(1)).save(any());
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

        verify(eventPublisher, never()).publish(any());
    }
}