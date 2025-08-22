package com.commerce.product.application.usecase;

import com.commerce.product.domain.event.ProductUpdatedEvent;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.exception.InvalidProductIdException;
import com.commerce.product.domain.exception.InvalidProductNameException;
import com.commerce.product.application.service.UpdateProductService;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    private UpdateProductUseCase updateProductUseCase;

    @BeforeEach
    void setUp() {
        updateProductUseCase = new UpdateProductService(productRepository);
    }

    @Test
    @DisplayName("정상적인 상품 수정 요청이 주어지면 상품이 수정된다")
    void givenValidUpdateRequest_whenUpdateProduct_thenProductIsUpdated() {
        // Given
        ProductId productId = ProductId.generate();
        Product existingProduct = Product.create(
            new ProductName("Original Product"),
            "Original description",
            ProductType.NORMAL
        );
        existingProduct.clearDomainEvents(); // 생성 이벤트 제거

        UpdateProductRequest request = UpdateProductRequest.builder()
            .productId(productId.value())
            .name("Updated Product")
            .description("Updated description")
            .build();

        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UpdateProductResponse response = updateProductUseCase.updateProduct(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(existingProduct.getId().value());
        assertThat(response.getName()).isEqualTo("Updated Product");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getType()).isEqualTo(ProductType.NORMAL.name());
        assertThat(response.getStatus()).isEqualTo(existingProduct.getStatus().name());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getName().value()).isEqualTo("Updated Product");
        assertThat(savedProduct.getDescription()).isEqualTo("Updated description");
        assertThat(savedProduct.getDomainEvents()).hasSize(1);
        assertThat(savedProduct.getDomainEvents().get(0)).isInstanceOf(ProductUpdatedEvent.class);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 수정 요청하면 예외가 발생한다")
    void givenNonExistentProductId_whenUpdateProduct_thenThrowsException() {
        // Given
        String nonExistentId = ProductId.generate().value();
        UpdateProductRequest request = UpdateProductRequest.builder()
            .productId(nonExistentId)
            .name("Updated Product")
            .description("Updated description")
            .build();

        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> updateProductUseCase.updateProduct(request))
            .isInstanceOf(InvalidProductException.class)
            .hasMessage("Product not found with id: " + nonExistentId);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("삭제된 상품을 수정하려고 하면 예외가 발생한다")
    void givenDeletedProduct_whenUpdateProduct_thenThrowsException() {
        // Given
        ProductId productId = ProductId.generate();
        Product deletedProduct = Product.create(
            new ProductName("Deleted Product"),
            "Deleted description",
            ProductType.NORMAL
        );
        deletedProduct.delete();
        deletedProduct.clearDomainEvents();

        UpdateProductRequest request = UpdateProductRequest.builder()
            .productId(productId.value())
            .name("Updated Product")
            .description("Updated description")
            .build();

        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(deletedProduct));

        // When & Then
        assertThatThrownBy(() -> updateProductUseCase.updateProduct(request))
            .isInstanceOf(InvalidProductException.class)
            .hasMessage("Cannot update deleted product");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("유효하지 않은 상품 ID 형식으로 수정 요청하면 예외가 발생한다")
    void givenInvalidProductIdFormat_whenUpdateProduct_thenThrowsException() {
        // Given
        UpdateProductRequest request = UpdateProductRequest.builder()
            .productId("invalid-id")
            .name("Updated Product")
            .description("Updated description")
            .build();

        // When & Then
        assertThatThrownBy(() -> updateProductUseCase.updateProduct(request))
            .isInstanceOf(InvalidProductIdException.class);

        verify(productRepository, never()).findById(any(ProductId.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("유효하지 않은 상품명으로 수정 요청하면 예외가 발생한다")
    void givenInvalidProductName_whenUpdateProduct_thenThrowsException() {
        // Given
        ProductId productId = ProductId.generate();
        Product existingProduct = Product.create(
            new ProductName("Original Product"),
            "Original description",
            ProductType.NORMAL
        );

        UpdateProductRequest request = UpdateProductRequest.builder()
            .productId(productId.value())
            .name("")  // 빈 상품명
            .description("Updated description")
            .build();

        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));

        // When & Then
        assertThatThrownBy(() -> updateProductUseCase.updateProduct(request))
            .isInstanceOf(InvalidProductNameException.class);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("상품명만 수정하면 설명은 기존 값을 유지한다")
    void givenOnlyNameUpdate_whenUpdateProduct_thenDescriptionRemainsSame() {
        // Given
        ProductId productId = ProductId.generate();
        Product existingProduct = Product.create(
            new ProductName("Original Product"),
            "Original description",
            ProductType.NORMAL
        );
        existingProduct.clearDomainEvents();

        UpdateProductRequest request = UpdateProductRequest.builder()
            .productId(productId.value())
            .name("Updated Product")
            .description(null)  // null로 설정
            .build();

        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UpdateProductResponse response = updateProductUseCase.updateProduct(request);

        // Then
        assertThat(response.getName()).isEqualTo("Updated Product");
        assertThat(response.getDescription()).isEqualTo("Original description");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getDescription()).isEqualTo("Original description");
    }

    @Test
    @DisplayName("설명만 수정하면 상품명은 기존 값을 유지한다")
    void givenOnlyDescriptionUpdate_whenUpdateProduct_thenNameRemainsSame() {
        // Given
        ProductId productId = ProductId.generate();
        Product existingProduct = Product.create(
            new ProductName("Original Product"),
            "Original description",
            ProductType.NORMAL
        );
        existingProduct.clearDomainEvents();

        UpdateProductRequest request = UpdateProductRequest.builder()
            .productId(productId.value())
            .name(null)  // null로 설정
            .description("Updated description")
            .build();

        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UpdateProductResponse response = updateProductUseCase.updateProduct(request);

        // Then
        assertThat(response.getName()).isEqualTo("Original Product");
        assertThat(response.getDescription()).isEqualTo("Updated description");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getName().value()).isEqualTo("Original Product");
    }

    @Test
    @DisplayName("활성화된 상품도 수정할 수 있다")
    void givenActiveProduct_whenUpdateProduct_thenProductIsUpdated() {
        // Given
        ProductId productId = ProductId.generate();
        Product activeProduct = Product.create(
            new ProductName("Active Product"),
            "Active description",
            ProductType.NORMAL
        );
        activeProduct.addOption(ProductOption.single(
            "Default Option",
            Money.of(BigDecimal.valueOf(10000), Currency.KRW),
            "SKU123"
        ));
        activeProduct.activate();
        activeProduct.clearDomainEvents();

        UpdateProductRequest request = UpdateProductRequest.builder()
            .productId(productId.value())
            .name("Updated Active Product")
            .description("Updated active description")
            .build();

        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UpdateProductResponse response = updateProductUseCase.updateProduct(request);

        // Then
        assertThat(response.getStatus()).isEqualTo(ProductStatus.ACTIVE.name());
        assertThat(response.getName()).isEqualTo("Updated Active Product");
        assertThat(response.getDescription()).isEqualTo("Updated active description");

        verify(productRepository).save(any(Product.class));
    }
}