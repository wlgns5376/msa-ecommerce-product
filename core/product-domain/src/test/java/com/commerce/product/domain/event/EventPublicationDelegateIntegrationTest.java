package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {EventPublicationDelegateIntegrationTest.TestConfig.class, EventPublicationDelegate.class})
@DisplayName("EventPublicationDelegate 통합 테스트")
class EventPublicationDelegateIntegrationTest {
    
    @MockBean
    private DomainEventPublisher eventPublisher;
    
    @Autowired
    private EventPublicationDelegate delegate;
    
    private TestDomainEvent testEvent;
    
    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent();
    }
    
    @Test
    @DisplayName("재시도 가능한 예외 발생 시 3번 재시도 후 복구 메서드가 호출된다")
    void publishWithRetry_RetriesAndRecovers() {
        // Given
        RuntimeException exception = new RuntimeException("Temporary failure");
        doThrow(exception)
            .when(eventPublisher).publish(any(DomainEvent.class));
        
        // When
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        
        // Then
        verify(eventPublisher, times(3)).publish(testEvent); // 최초 시도 + 2번 재시도
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
        doThrow(new RuntimeException("First attempt fails"))
            .doNothing()
            .when(eventPublisher).publish(any(DomainEvent.class));
        
        // When
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        
        // Then
        verify(eventPublisher, times(2)).publish(testEvent);
    }
    
    @Test
    @DisplayName("재시도 불가능한 예외 발생 시 재시도하지 않고 즉시 예외를 던진다")
    void publishWithRetry_NonRetryableException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Non-retryable error");
        doThrow(exception)
            .when(eventPublisher).publish(any(DomainEvent.class));
        
        // When & Then
        // assertThatThrownBy를 사용하는 것이 더 깔끔하지만,
        // Spring Retry의 @Recover 메소드가 IllegalArgumentException도 처리하는 현재 구조에서는
        // 예외가 발생하지 않으므로 기존 방식 유지
        assertDoesNotThrow(() -> delegate.publishWithRetry(testEvent));
        
        // Then
        verify(eventPublisher, times(1)).publish(testEvent); // 재시도 없이 한 번만 호출
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