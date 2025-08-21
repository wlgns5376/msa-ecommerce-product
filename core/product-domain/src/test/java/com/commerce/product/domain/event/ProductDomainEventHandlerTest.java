package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

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
    void handleDomainEvent_ShouldDelegateToEventPublicationDelegate() throws IOException, TimeoutException {
        // Given
        doNothing().when(delegate).publishWithRetry(any(DomainEvent.class));
        
        // When
        handler.handleDomainEvent(testEvent);
        
        // Then
        verify(delegate, times(1)).publishWithRetry(testEvent);
    }
    
    @Test
    @DisplayName("이벤트 발행 실패 시 RuntimeException으로 감싸서 던진다")
    void handleDomainEvent_WhenPublishFails_ThrowsRuntimeException() throws IOException, TimeoutException {
        // Given
        IOException ioException = new IOException("Network error");
        doThrow(ioException).when(delegate).publishWithRetry(any(DomainEvent.class));
        
        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, 
            () -> handler.handleDomainEvent(testEvent));
        
        assertEquals("Failed to publish domain event", thrown.getMessage());
        assertEquals(ioException, thrown.getCause());
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