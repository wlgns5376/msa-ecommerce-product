package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductDomainEventHandlerTest {

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private DomainEvent domainEvent;

    private ProductDomainEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new ProductDomainEventHandler(eventPublisher);
    }

    @Test
    @DisplayName("도메인 이벤트 발행 성공")
    void handleDomainEvent_Success() {
        // When
        eventHandler.handleDomainEvent(domainEvent);

        // Then
        verify(eventPublisher).publish(domainEvent);
    }

    @Test
    @DisplayName("도메인 이벤트 발행 실패 시 RuntimeException 발생")
    void handleDomainEvent_WhenPublishFails_ThrowsRuntimeException() {
        // Given
        String errorMessage = "Failed to publish event";
        doThrow(new RuntimeException(errorMessage))
            .when(eventPublisher).publish(domainEvent);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> eventHandler.handleDomainEvent(domainEvent));
        
        // Verify
        verify(eventPublisher).publish(domainEvent);
    }

    static class TestDomainEvent implements DomainEvent {
        @Override
        public LocalDateTime getOccurredAt() {
            return LocalDateTime.now();
        }

        @Override
        public String eventType() {
            return "TestDomainEvent";
        }
    }
}