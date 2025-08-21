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
import org.springframework.dao.TransientDataAccessException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublicationDelegateTest {
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    @Mock
    private DeadLetterQueueService deadLetterQueueService;
    
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
    @DisplayName("재시도 불가능한 예외 발생 시 즉시 예외가 전파된다")
    void publishWithRetry_NonRetryableException_ThrowsImmediately() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        doThrow(exception).when(eventPublisher).publish(any(DomainEvent.class));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> delegate.publishWithRetry(testEvent));
        verify(eventPublisher, times(1)).publish(testEvent);
    }
    
    @Test
    @DisplayName("RuntimeException으로 감싸진 IOException 발생 시 재시도 가능한 예외로 처리된다")
    void publishWithRetry_IOException_Retryable() {
        // Given
        RuntimeException wrappedException = new RuntimeException("Network error", new IOException("IO error"));
        doThrow(wrappedException).when(eventPublisher).publish(any(DomainEvent.class));
        
        // When & Then
        assertThrows(IOException.class, () -> delegate.publishWithRetry(testEvent));
        verify(eventPublisher, times(1)).publish(testEvent);
    }
    
    @Test
    @DisplayName("RuntimeException으로 감싸진 TimeoutException 발생 시 재시도 가능한 예외로 처리된다")
    void publishWithRetry_TimeoutException_Retryable() {
        // Given
        RuntimeException wrappedException = new RuntimeException("Timeout error", new TimeoutException("Timeout occurred"));
        doThrow(wrappedException).when(eventPublisher).publish(any(DomainEvent.class));
        
        // When & Then
        assertThrows(TimeoutException.class, () -> delegate.publishWithRetry(testEvent));
        verify(eventPublisher, times(1)).publish(testEvent);
    }
    
    @Test
    @DisplayName("TransientDataAccessException 발생 시 재시도 가능한 예외로 처리된다")
    void publishWithRetry_TransientDataAccessException_Retryable() {
        // Given
        TransientDataAccessException exception = new TransientDataAccessException("DB connection error") {};
        doThrow(exception).when(eventPublisher).publish(any(DomainEvent.class));
        
        // When & Then
        assertThrows(TransientDataAccessException.class, () -> delegate.publishWithRetry(testEvent));
        verify(eventPublisher, times(1)).publish(testEvent);
    }
    
    
    
    @Test
    @DisplayName("IOException 재시도 실패 시 DLQ에 저장된다")
    void handleFailedEvent_IOException_StoresInDLQ() {
        // Given
        IOException exception = new IOException("Persistent network failure");
        doNothing().when(deadLetterQueueService).storeFailedEvent(any(DomainEvent.class), any(Exception.class));
        
        // When & Then
        assertDoesNotThrow(() -> delegate.handleFailedEvent(exception, testEvent));
        verify(deadLetterQueueService, times(1)).storeFailedEvent(testEvent, exception);
    }
    
    @Test
    @DisplayName("TimeoutException 재시도 실패 시 DLQ에 저장된다")
    void handleFailedEvent_TimeoutException_StoresInDLQ() {
        // Given
        TimeoutException exception = new TimeoutException("Persistent timeout");
        doNothing().when(deadLetterQueueService).storeFailedEvent(any(DomainEvent.class), any(Exception.class));
        
        // When & Then
        assertDoesNotThrow(() -> delegate.handleFailedEvent(exception, testEvent));
        verify(deadLetterQueueService, times(1)).storeFailedEvent(testEvent, exception);
    }
    
    @Test
    @DisplayName("TransientDataAccessException 재시도 실패 시 DLQ에 저장된다")
    void handleFailedEvent_TransientDataAccessException_StoresInDLQ() {
        // Given
        TransientDataAccessException exception = new TransientDataAccessException("Persistent DB error") {};
        doNothing().when(deadLetterQueueService).storeFailedEvent(any(DomainEvent.class), any(Exception.class));
        
        // When & Then
        assertDoesNotThrow(() -> delegate.handleFailedEvent(exception, testEvent));
        verify(deadLetterQueueService, times(1)).storeFailedEvent(testEvent, exception);
    }
    
    @Test
    @DisplayName("DLQ 저장 실패 시 EventPublicationException이 발생한다")
    void handleFailedEvent_DLQFailure_ThrowsEventPublicationException() {
        // Given
        IOException originalException = new IOException("Network failure");
        RuntimeException dlqException = new RuntimeException("DLQ storage failure");
        doThrow(dlqException).when(deadLetterQueueService).storeFailedEvent(any(DomainEvent.class), any(Exception.class));
        
        // When & Then
        EventPublicationException thrown = assertThrows(EventPublicationException.class, 
            () -> delegate.handleFailedEvent(originalException, testEvent));
        
        assertEquals("Failed to publish event and store in DLQ", thrown.getMessage());
        assertEquals(testEvent, thrown.getFailedEvent());
        assertEquals(dlqException, thrown.getCause());
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