package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.retry.ExhaustedRetryException;

@SpringBootTest(classes = {EventPublicationDelegateIntegrationTest.TestConfig.class, EventPublicationDelegate.class, InMemoryDeadLetterQueueService.class})
@DisplayName("EventPublicationDelegate 통합 테스트")
class EventPublicationDelegateIntegrationTest {
    
    @MockBean
    private DomainEventPublisher eventPublisher;
    
    @MockBean
    private DeadLetterQueueService deadLetterQueueService;
    
    @Autowired
    private EventPublicationDelegate delegate;
    
    private TestDomainEvent testEvent;
    
    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent();
    }
    
    @Test
    @DisplayName("IOException 발생 시 3번 재시도 후 DLQ에 저장된다")
    void publishWithRetry_IOException_RetriesAndStoresInDLQ() {
        // Given
        IOException ioException = new IOException("Network failure");
        RuntimeException wrappedException = new RuntimeException("Network error", ioException);
        doThrow(wrappedException)
            .when(eventPublisher).publish(any(DomainEvent.class));
        doNothing().when(deadLetterQueueService).storeFailedEvent(any(DomainEvent.class), any(Exception.class));
        
        // When
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        
        // Then
        verify(eventPublisher, times(3)).publish(testEvent); // 최초 시도 + 2번 재시도
        verify(deadLetterQueueService, times(1)).storeFailedEvent(eq(testEvent), any(IOException.class));
    }
    
    @Test
    @DisplayName("TimeoutException 발생 시 3번 재시도 후 DLQ에 저장된다")
    void publishWithRetry_TimeoutException_RetriesAndStoresInDLQ() {
        // Given
        TimeoutException timeoutException = new TimeoutException("Timeout occurred");
        RuntimeException wrappedException = new RuntimeException("Timeout error", timeoutException);
        doThrow(wrappedException)
            .when(eventPublisher).publish(any(DomainEvent.class));
        doNothing().when(deadLetterQueueService).storeFailedEvent(any(DomainEvent.class), any(Exception.class));
        
        // When
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        
        // Then
        verify(eventPublisher, times(3)).publish(testEvent);
        verify(deadLetterQueueService, times(1)).storeFailedEvent(eq(testEvent), any(TimeoutException.class));
    }
    
    @Test
    @DisplayName("TransientDataAccessException 발생 시 3번 재시도 후 DLQ에 저장된다")
    void publishWithRetry_TransientDataAccessException_RetriesAndStoresInDLQ() {
        // Given
        TransientDataAccessException exception = new TransientDataAccessException("DB connection error") {};
        doThrow(exception)
            .when(eventPublisher).publish(any(DomainEvent.class));
        doNothing().when(deadLetterQueueService).storeFailedEvent(any(DomainEvent.class), any(Exception.class));
        
        // When
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        
        // Then
        verify(eventPublisher, times(3)).publish(testEvent);
        verify(deadLetterQueueService, times(1)).storeFailedEvent(eq(testEvent), any(TransientDataAccessException.class));
    }
    
    @Test
    @DisplayName("첫 번째 시도에서 성공하면 재시도하지 않는다")
    void publishWithRetry_SuccessOnFirstAttempt() {
        // Given
        doNothing().when(eventPublisher).publish(any(DomainEvent.class));
        
        // When
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        
        // Then
        verify(eventPublisher, times(1)).publish(testEvent);
    }
    
    @Test
    @DisplayName("두 번째 시도에서 성공하면 추가 재시도하지 않는다")
    void publishWithRetry_SuccessOnSecondAttempt() {
        // Given
        IOException ioException = new IOException("First attempt fails");
        RuntimeException wrappedException = new RuntimeException("First attempt error", ioException);
        doThrow(wrappedException)
            .doNothing()
            .when(eventPublisher).publish(any(DomainEvent.class));
        
        // When
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        
        // Then
        verify(eventPublisher, times(2)).publish(testEvent);
        verifyNoInteractions(deadLetterQueueService);
    }
    
    @Test
    @DisplayName("재시도 불가능한 예외 발생 시 재시도하지 않고 즉시 예외를 던진다")
    void publishWithRetry_NonRetryableException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Non-retryable error");
        doThrow(exception)
            .when(eventPublisher).publish(any(DomainEvent.class));
        
        // When & Then
        // Spring Retry가 IllegalArgumentException을 재시도하지 않으므로 ExhaustedRetryException이 발생
        // 이는 @Recover 메서드가 IllegalArgumentException을 처리하지 않기 때문
        Exception thrown = assertThrows(Exception.class, () -> delegate.publishWithRetry(testEvent));
        
        // Then
        // 재시도가 시도되었는지 확인
        assertTrue(thrown instanceof IllegalArgumentException || 
                   thrown.getCause() instanceof IllegalArgumentException ||
                   thrown instanceof ExhaustedRetryException);
        
        verify(eventPublisher, times(1)).publish(testEvent); // 재시도 없이 한 번만 호출
        verifyNoInteractions(deadLetterQueueService); // DLQ 호출 없음
    }
    
    @Test
    @DisplayName("DLQ 저장 실패 시 EventPublicationException이 발생한다")
    void publishWithRetry_DLQFailure_ThrowsEventPublicationException() {
        // Given
        IOException originalException = new IOException("Network failure");
        RuntimeException wrappedException = new RuntimeException("Network error", originalException);
        RuntimeException dlqException = new RuntimeException("DLQ storage failure");
        
        doThrow(wrappedException)
            .when(eventPublisher).publish(any(DomainEvent.class));
        doThrow(dlqException)
            .when(deadLetterQueueService).storeFailedEvent(any(DomainEvent.class), any(Exception.class));
        
        // When & Then
        EventPublicationException thrown = assertThrows(EventPublicationException.class, 
            () -> delegate.publishWithRetry(testEvent));
        
        // Then
        assertEquals("Failed to publish event and store in DLQ", thrown.getMessage());
        assertEquals(testEvent, thrown.getFailedEvent());
        assertEquals(dlqException, thrown.getCause());
        
        verify(eventPublisher, times(3)).publish(testEvent); // 3번 재시도
        verify(deadLetterQueueService, times(1)).storeFailedEvent(eq(testEvent), any(IOException.class));
    }
    
    @Configuration
    @EnableRetry
    static class TestConfig {
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