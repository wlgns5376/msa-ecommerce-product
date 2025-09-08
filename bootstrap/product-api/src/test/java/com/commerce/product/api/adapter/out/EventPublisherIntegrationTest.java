package com.commerce.product.api.adapter.out;

import com.commerce.common.event.DomainEvent;
import com.commerce.product.application.service.port.out.EventPublisher;
import com.commerce.product.domain.event.*;
import com.commerce.product.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisher 통합 테스트")
class EventPublisherIntegrationTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    
    @InjectMocks
    private EventPublisherAdapter eventPublisher;

    private ProductId productId;
    private Product product;
    private CategoryId categoryId;

    @BeforeEach
    void setUp() {
        productId = new ProductId(UUID.randomUUID().toString());
        product = new Product(
            productId,
            ProductName.of("테스트 상품"),
            "테스트 상품 설명",
            ProductType.NORMAL
        );
        categoryId = new CategoryId(UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("상품 생성 이벤트를 발행한다")
    void testPublishProductCreatedEvent() {
        // Given
        ProductCreatedEvent event = new ProductCreatedEvent(
            productId,
            product.getName().value(),
            product.getType()
        );

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductCreatedEvent.class));
    }

    @Test
    @DisplayName("상품 수정 이벤트를 발행한다")
    void testPublishProductUpdatedEvent() {
        // Given
        ProductUpdatedEvent event = new ProductUpdatedEvent(
            productId,
            "수정된 상품명",
            "수정된 설명"
        );

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductUpdatedEvent.class));
    }

    @Test
    @DisplayName("상품 옵션 추가 이벤트를 발행한다")
    void testPublishProductOptionAddedEvent() {
        // Given
        ProductOption option = ProductOption.single(
            "옵션1",
            Money.of(10000L),
            "SKU-001"
        );
        ProductOptionAddedEvent event = new ProductOptionAddedEvent(productId, option);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductOptionAddedEvent.class));
    }

    @Test
    @DisplayName("상품 품절 이벤트를 발행한다")
    void testPublishProductOutOfStockEvent() {
        // Given
        ProductOutOfStockEvent event = new ProductOutOfStockEvent(productId);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductOutOfStockEvent.class));
    }

    @Test
    @DisplayName("상품 재입고 이벤트를 발행한다")
    void testPublishProductInStockEvent() {
        // Given
        ProductInStockEvent event = new ProductInStockEvent(productId);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductInStockEvent.class));
    }

    @Test
    @DisplayName("상품 활성화 이벤트를 발행한다")
    void testPublishProductActivatedEvent() {
        // Given
        ProductActivatedEvent event = new ProductActivatedEvent(productId);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductActivatedEvent.class));
    }

    @Test
    @DisplayName("상품 비활성화 이벤트를 발행한다")
    void testPublishProductDeactivatedEvent() {
        // Given
        ProductDeactivatedEvent event = new ProductDeactivatedEvent(productId);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductDeactivatedEvent.class));
    }

    @Test
    @DisplayName("카테고리 활성화 이벤트를 발행한다")
    void testPublishCategoryActivatedEvent() {
        // Given
        CategoryActivatedEvent event = new CategoryActivatedEvent(categoryId);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(CategoryActivatedEvent.class));
    }

    @Test
    @DisplayName("카테고리 비활성화 이벤트를 발행한다")
    void testPublishCategoryDeactivatedEvent() {
        // Given
        CategoryDeactivatedEvent event = new CategoryDeactivatedEvent(categoryId);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(CategoryDeactivatedEvent.class));
    }

    @Test
    @DisplayName("여러 도메인 이벤트를 순차적으로 발행한다")
    void testPublishMultipleEvents() {
        // Given
        List<DomainEvent> events = Arrays.asList(
            new ProductCreatedEvent(productId, product.getName().value(), product.getType()),
            new ProductUpdatedEvent(productId, "수정된 상품명", "수정된 설명"),
            new ProductOutOfStockEvent(productId)
        );

        // When
        eventPublisher.publishAll(events);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductCreatedEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductUpdatedEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductOutOfStockEvent.class));
    }

    @Test
    @DisplayName("이벤트 발행시 이벤트 타입과 집계 ID가 올바르게 설정된다")
    void testEventMetadata() {
        // Given
        ProductCreatedEvent event = new ProductCreatedEvent(
            productId,
            product.getName().value(),
            product.getType()
        );

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher).publishEvent(any(ProductCreatedEvent.class));
    }

    @Test
    @DisplayName("복합 이벤트 시나리오: 상품 생성 -> 옵션 추가 -> 품절 -> 재입고")
    void testComplexEventScenario() {
        // Given
        ProductOption option1 = ProductOption.single(
            "옵션1",
            Money.of(10000L),
            "SKU-001"
        );
        
        ProductOption option2 = ProductOption.single(
            "옵션2",
            Money.of(20000L),
            "SKU-002"
        );

        // When
        // 상품 생성
        eventPublisher.publish(new ProductCreatedEvent(productId, product.getName().value(), product.getType()));
        
        // 옵션 추가
        eventPublisher.publish(new ProductOptionAddedEvent(productId, option1));
        eventPublisher.publish(new ProductOptionAddedEvent(productId, option2));
        
        // 품절
        eventPublisher.publish(new ProductOutOfStockEvent(productId));
        
        // 재입고
        eventPublisher.publish(new ProductInStockEvent(productId));

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductCreatedEvent.class));
        verify(applicationEventPublisher, times(2)).publishEvent(any(ProductOptionAddedEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductOutOfStockEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(ProductInStockEvent.class));
    }

    @Test
    @DisplayName("번들 예약 완료 이벤트를 발행한다")
    void testPublishBundleReservationCompletedEvent() {
        // Given
        String sagaId = UUID.randomUUID().toString();
        BundleReservationCompletedEvent event = new BundleReservationCompletedEvent(
            sagaId,
            productId.value(),
            SkuMapping.single("SKU-001")
        );

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(BundleReservationCompletedEvent.class));
    }

    @Test
    @DisplayName("번들 예약 실패 이벤트를 발행한다")
    void testPublishBundleReservationFailedEvent() {
        // Given
        String sagaId = UUID.randomUUID().toString();
        BundleReservationFailedEvent event = new BundleReservationFailedEvent(
            sagaId, 
            productId.value(), 
            "재고 부족"
        );

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(BundleReservationFailedEvent.class));
    }
}