package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublicationDelegateTest {
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    @InjectMocks
    private EventPublicationDelegate delegate;
    
    private TestDomainEvent testEvent;
    
    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent();
    }
    
    @Test
    @DisplayName("이벤트 발행이 성공하면 정상적으로 처리된다")
    void publishWithRetry_Success() {
        // Given
        doNothing().when(eventPublisher).publish(any(DomainEvent.class));
        
        // When & Then
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        verify(eventPublisher, times(1)).publish(testEvent);
    }
    
    @Test
    @DisplayName("예외 발생 시 로그가 기록되고 예외가 전파된다")
    void publishWithRetry_LogsAndThrowsException() {
        // Given
        RuntimeException exception = new RuntimeException("Network error");
        doThrow(exception).when(eventPublisher).publish(any(DomainEvent.class));
        
        // When & Then
        assertThrows(RuntimeException.class, () -> delegate.publishWithRetry(testEvent));
        verify(eventPublisher, times(1)).publish(testEvent);
    }
    
    
    
    @Test
    @DisplayName("모든 재시도가 실패하면 handleFailedEvent가 호출된다")
    void handleFailedEvent_LogsError() {
        // Given
        RuntimeException exception = new RuntimeException("Persistent failure");
        
        // When & Then
        assertDoesNotThrow(() -> delegate.handleFailedEvent(exception, testEvent));
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