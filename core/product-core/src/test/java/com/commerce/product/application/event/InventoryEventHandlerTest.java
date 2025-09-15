package com.commerce.product.application.event;

import com.commerce.product.domain.event.ProductInStockEvent;
import com.commerce.product.domain.event.ProductOutOfStockEvent;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InventoryEventHandler inventoryEventHandler;

    private Product product;
    private String skuId;

    @BeforeEach
    void setUp() {
        skuId = "SKU001";
        product = Product.create(
                new ProductName("테스트 상품"),
                "테스트 설명",
                ProductType.NORMAL
        );
        
        // 단일 SKU 옵션 추가
        product.addOption(ProductOption.single(
                "기본 옵션",
                Money.of(new BigDecimal("10000"), Currency.KRW),
                skuId
        ));
    }

    @Test
    @DisplayName("재고 부족 이벤트 발생 시 상품을 품절 상태로 변경한다")
    void handleStockDepletedEvent_shouldMarkProductAsOutOfStock() {
        // Given
        when(productRepository.findProductsBySkuId(skuId)).thenReturn(Collections.singletonList(product));

        // When
        inventoryEventHandler.handleStockDepletedEvent(skuId);

        // Then
        verify(productRepository).findProductsBySkuId(skuId);
        verify(productRepository).save(product);
        
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());
        
        List<Object> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents).hasAtLeastOneElementOfType(ProductOutOfStockEvent.class);
        assertThat(product.isOutOfStock()).isTrue();
    }

    @Test
    @DisplayName("이미 품절 상태인 상품은 중복 처리하지 않는다")
    void handleStockDepletedEvent_shouldNotProcessAlreadyOutOfStockProduct() {
        // Given
        product.markAsOutOfStock();
        product.pullDomainEvents(); // 이전 이벤트 제거
        when(productRepository.findProductsBySkuId(skuId)).thenReturn(Collections.singletonList(product));

        // When
        inventoryEventHandler.handleStockDepletedEvent(skuId);

        // Then
        verify(productRepository).findProductsBySkuId(skuId);
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("여러 상품이 동일한 SKU를 사용하는 경우 모두 품절 처리한다")
    void handleStockDepletedEvent_shouldMarkMultipleProductsAsOutOfStock() {
        // Given
        Product product2 = Product.create(
                new ProductName("테스트 상품 2"),
                "테스트 설명 2",
                ProductType.NORMAL
        );
        product2.addOption(ProductOption.single(
                "공통 옵션",
                Money.of(new BigDecimal("20000"), Currency.KRW),
                skuId
        ));

        when(productRepository.findProductsBySkuId(skuId))
                .thenReturn(Arrays.asList(product, product2));

        // When
        inventoryEventHandler.handleStockDepletedEvent(skuId);

        // Then
        verify(productRepository, times(2)).save(any(Product.class));
        assertThat(product.isOutOfStock()).isTrue();
        assertThat(product2.isOutOfStock()).isTrue();
    }

    @Test
    @DisplayName("재고 복원 이벤트 발생 시 상품을 재고 있음 상태로 변경한다")
    void handleStockReplenishedEvent_shouldMarkProductAsInStock() {
        // Given
        product.markAsOutOfStock();
        product.pullDomainEvents();
        when(productRepository.findProductsBySkuId(skuId)).thenReturn(Collections.singletonList(product));

        // When
        inventoryEventHandler.handleStockReplenishedEvent(skuId);

        // Then
        verify(productRepository).findProductsBySkuId(skuId);
        verify(productRepository).save(product);
        
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());
        
        List<Object> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents).hasAtLeastOneElementOfType(ProductInStockEvent.class);
        assertThat(product.isOutOfStock()).isFalse();
    }

    @Test
    @DisplayName("이미 재고 있음 상태인 상품은 중복 처리하지 않는다")
    void handleStockReplenishedEvent_shouldNotProcessAlreadyInStockProduct() {
        // Given
        // product는 기본적으로 재고 있음 상태
        when(productRepository.findProductsBySkuId(skuId)).thenReturn(Collections.singletonList(product));

        // When
        inventoryEventHandler.handleStockReplenishedEvent(skuId);

        // Then
        verify(productRepository).findProductsBySkuId(skuId);
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("예외 발생 시 원본 예외를 다시 던진다")
    void handleStockDepletedEvent_shouldRethrowException() {
        // Given
        RuntimeException exception = new RuntimeException("Database error");
        when(productRepository.findProductsBySkuId(skuId)).thenThrow(exception);

        // When & Then
        assertThatThrownBy(() -> inventoryEventHandler.handleStockDepletedEvent(skuId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    @DisplayName("SKU와 연관된 상품이 없는 경우 정상적으로 처리된다")
    void handleStockDepletedEvent_shouldHandleNoProductsGracefully() {
        // Given
        when(productRepository.findProductsBySkuId(skuId)).thenReturn(Collections.emptyList());

        // When
        inventoryEventHandler.handleStockDepletedEvent(skuId);

        // Then
        verify(productRepository).findProductsBySkuId(skuId);
        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}