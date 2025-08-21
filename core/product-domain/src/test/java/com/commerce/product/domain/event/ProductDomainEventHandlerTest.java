package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class ProductDomainEventHandlerTest {
    
    @Mock
    private EventPublicationDelegate delegate;
    
    @InjectMocks
    private ProductDomainEventHandler handler;
    
    private TestDomainEvent testEvent;
    
    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent();
    }
    
    @Test
    @DisplayName("도메인 이벤트 수신 시 EventPublicationDelegate를 통해 이벤트가 발행된다")
    void handleDomainEvent_ShouldDelegateToEventPublicationDelegate() {
        // Given - 테스트 이벤트가 준비됨
        
        // When
        handler.handleDomainEvent(testEvent);
        
        // Then
        verify(delegate, times(1)).publishWithRetry(testEvent);
    }
    
    static class TestDomainEvent implements DomainEvent {
        @Override
        public LocalDateTime getOccurredAt() {
            return LocalDateTime.now();
        }
        
        @Override
        public String eventType() {
            return "TestEvent";
        }
    }
}