package com.commerce.product.domain.event;

import com.commerce.product.domain.model.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductOutOfStockEventTest {

    @Test
    @DisplayName("ProductOutOfStockEvent 생성 시 productId가 정상적으로 설정된다")
    void shouldCreateEventWithProductId() {
        // Given
        ProductId productId = ProductId.generate();

        // When
        ProductOutOfStockEvent event = new ProductOutOfStockEvent(productId);

        // Then
        assertThat(event.getProductId()).isEqualTo(productId);
        assertThat(event.getAggregateId()).isEqualTo(productId.value());
    }

    @Test
    @DisplayName("ProductOutOfStockEvent의 이벤트 타입이 올바르게 반환된다")
    void shouldReturnCorrectEventType() {
        // Given
        ProductId productId = ProductId.generate();

        // When
        ProductOutOfStockEvent event = new ProductOutOfStockEvent(productId);

        // Then
        assertThat(event.getEventType()).isEqualTo("product.out.of.stock");
        assertThat(event.eventType()).isEqualTo("product.out.of.stock");
    }

    @Test
    @DisplayName("ProductOutOfStockEvent 생성 시 이벤트 ID와 발생 시간이 자동으로 설정된다")
    void shouldAutoGenerateEventIdAndOccurredAt() {
        // Given
        ProductId productId = ProductId.generate();
        LocalDateTime beforeCreation = LocalDateTime.now();

        // When
        ProductOutOfStockEvent event = new ProductOutOfStockEvent(productId);

        // Then
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getOccurredAt()).isAfterOrEqualTo(beforeCreation);
        assertThat(event.getOccurredAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(event.occurredAt()).isEqualTo(event.getOccurredAt());
    }

    @Test
    @DisplayName("동일한 ProductId로 생성된 여러 이벤트는 서로 다른 이벤트 ID를 가진다")
    void shouldHaveDifferentEventIdsForSameProductId() {
        // Given
        ProductId productId = ProductId.generate();

        // When
        ProductOutOfStockEvent event1 = new ProductOutOfStockEvent(productId);
        ProductOutOfStockEvent event2 = new ProductOutOfStockEvent(productId);

        // Then
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        assertThat(event1.getAggregateId()).isEqualTo(event2.getAggregateId());
    }
}