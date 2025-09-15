package com.commerce.inventory.infrastructure.event.kafka;

import com.commerce.common.event.DomainEvent;
import com.commerce.inventory.infrastructure.event.kafka.retry.RetryableEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaErrorHandler 테스트")
class KafkaErrorHandlerTest {
    
    @Mock
    private RetryableEventStore retryableEventStore;
    
    private KafkaErrorHandler errorHandler;
    
    @BeforeEach
    void setUp() {
        errorHandler = new KafkaErrorHandler(retryableEventStore);
        
        // Set test configuration values
        ReflectionTestUtils.setField(errorHandler, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(errorHandler, "initialDelayMs", 100L);
        ReflectionTestUtils.setField(errorHandler, "maxDelayMs", 1000L);
        ReflectionTestUtils.setField(errorHandler, "delayMultiplier", 2.0);
    }
    
    @Test
    @DisplayName("재시도 가능한 에러인 경우 재시도를 스케줄링한다")
    void testHandleError_RetryableError() throws InterruptedException {
        // Given
        TestDomainEvent event = new TestDomainEvent();
        TimeoutException error = new TimeoutException("Network timeout");
        
        // When
        errorHandler.handleError(event, error);
        
        // Then - wait for scheduled retry
        Thread.sleep(200);
        verify(retryableEventStore, atLeastOnce()).retry(event);
    }
    
    @Test
    @DisplayName("재시도 불가능한 에러인 경우 DLQ로 이동한다")
    void testHandleError_NonRetryableError() throws InterruptedException {
        // Given
        TestDomainEvent event = new TestDomainEvent();
        IllegalArgumentException error = new IllegalArgumentException("Invalid argument");
        
        // When
        errorHandler.handleError(event, error);
        
        // Then - wait for async execution
        Thread.sleep(100);
        verify(retryableEventStore).moveToDeadLetter(eq(event), contains("Invalid argument"));
        verify(retryableEventStore, never()).retry(any());
    }
    
    @Test
    @DisplayName("RecordTooLargeException은 재시도하지 않는다")
    void testHandleError_RecordTooLarge() {
        // Given
        TestDomainEvent event = new TestDomainEvent();
        RuntimeException error = new RuntimeException("RecordTooLargeException: Message too large");
        
        // When
        errorHandler.handleError(event, error);
        
        // Then
        verify(retryableEventStore, timeout(1000)).moveToDeadLetter(eq(event), anyString());
        verify(retryableEventStore, never()).retry(any());
    }
    
    @Test
    @DisplayName("NetworkException은 재시도한다")
    void testHandleError_NetworkException() throws InterruptedException {
        // Given
        TestDomainEvent event = new TestDomainEvent();
        RuntimeException error = new RuntimeException("NetworkException: Connection failed");
        
        // When
        errorHandler.handleError(event, error);
        
        // Then - wait for scheduled retry
        Thread.sleep(200);
        verify(retryableEventStore, atLeastOnce()).retry(event);
    }
    
    @Test
    @DisplayName("NotLeaderForPartitionException은 재시도한다")
    void testHandleError_NotLeaderForPartition() throws InterruptedException {
        // Given
        TestDomainEvent event = new TestDomainEvent();
        RuntimeException error = new RuntimeException("NotLeaderForPartitionException");
        
        // When
        errorHandler.handleError(event, error);
        
        // Then - wait for scheduled retry
        Thread.sleep(200);
        verify(retryableEventStore, atLeastOnce()).retry(event);
    }
    
    @Test
    @DisplayName("null 메시지를 가진 에러는 재시도하지 않는다")
    void testHandleError_NullMessage() throws InterruptedException {
        // Given
        TestDomainEvent event = new TestDomainEvent();
        RuntimeException error = new RuntimeException((String) null);
        
        // When
        errorHandler.handleError(event, error);
        
        // Then - wait for async execution
        Thread.sleep(100);
        verify(retryableEventStore).moveToDeadLetter(eq(event), eq(null));
        verify(retryableEventStore, never()).retry(any());
    }
    
    @Test
    @DisplayName("shutdown 메서드가 정상적으로 실행된다")
    void testShutdown() {
        // When
        errorHandler.shutdown();
        
        // Then - no exception should be thrown
        // This test verifies that shutdown completes without error
    }
    
    // Test event class
    static class TestDomainEvent implements DomainEvent {
        private final LocalDateTime occurredAt = LocalDateTime.now();
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
        
        @Override
        public String eventType() {
            return "TestDomainEvent";
        }
    }
}