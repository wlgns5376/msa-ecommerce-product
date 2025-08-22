package com.commerce.product.application.aspect;

import com.commerce.common.event.DomainEvent;
import com.commerce.product.domain.event.ProductCreatedEvent;
import com.commerce.product.domain.model.AggregateRoot;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.model.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainEventPublishingAspectTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DomainEventPublishingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new DomainEventPublishingAspect(eventPublisher);
    }

    @Test
    @DisplayName("도메인 이벤트가 정상적으로 발행된다")
    void shouldPublishDomainEvents() {
        // Given
        TestAggregate aggregate = new TestAggregate();
        ProductCreatedEvent event1 = new ProductCreatedEvent(
            ProductId.generate(), 
            "Test Product", 
            ProductType.NORMAL
        );
        ProductCreatedEvent event2 = new ProductCreatedEvent(
            ProductId.generate(), 
            "Another Product", 
            ProductType.BUNDLE
        );
        
        aggregate.addTestEvent(event1);
        aggregate.addTestEvent(event2);

        // When
        aspect.publishDomainEvents(aggregate);

        // Then
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        
        List<DomainEvent> publishedEvents = eventCaptor.getAllValues();
        assertThat(publishedEvents).hasSize(2);
        assertThat(publishedEvents).containsExactly(event1, event2);
        
        // 이벤트가 clear 되었는지 확인
        assertThat(aggregate.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("이벤트가 없을 때는 아무것도 발행하지 않는다")
    void shouldNotPublishWhenNoEvents() {
        // Given
        TestAggregate aggregate = new TestAggregate();

        // When
        aspect.publishDomainEvents(aggregate);

        // Then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("savedAggregate가 null일 때는 아무것도 발행하지 않는다")
    void shouldNotPublishWhenSavedAggregateIsNull() {
        // When
        aspect.publishDomainEvents(null);

        // Then
        verify(eventPublisher, never()).publishEvent(any());
    }

    // 테스트용 Aggregate 클래스
    private static class TestAggregate extends AggregateRoot<String> {
        private final String id = "test-id";

        @Override
        public String getId() {
            return id;
        }

        public void addTestEvent(DomainEvent event) {
            addDomainEvent(event);
        }
    }
}