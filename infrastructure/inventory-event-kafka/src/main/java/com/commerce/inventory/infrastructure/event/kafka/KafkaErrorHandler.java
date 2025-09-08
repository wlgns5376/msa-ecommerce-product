package com.commerce.inventory.infrastructure.event.kafka;

import com.commerce.common.event.DomainEvent;
import com.commerce.inventory.infrastructure.event.kafka.retry.RetryableEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 이벤트 발행 실패 시 에러 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaErrorHandler {
    
    private final RetryableEventStore retryableEventStore;
    private final ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(2);
    
    @Value("${kafka.producer.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${kafka.producer.retry.initial-delay-ms:1000}")
    private long initialDelayMs;
    
    @Value("${kafka.producer.retry.max-delay-ms:60000}")
    private long maxDelayMs;
    
    @Value("${kafka.producer.retry.multiplier:2}")
    private double delayMultiplier;
    
    /**
     * 이벤트 발행 실패 처리
     */
    public void handleError(DomainEvent event, Throwable error) {
        log.error("Failed to publish event: {}, Error: {}", event.eventType(), error.getMessage());
        
        if (isRetryableError(error)) {
            scheduleRetry(event, 1);
        } else {
            handleNonRetryableError(event, error);
        }
    }
    
    /**
     * 재시도 가능한 에러인지 판단
     */
    private boolean isRetryableError(Throwable error) {
        // 네트워크 오류, 타임아웃 등 재시도 가능한 에러 판단
        String errorMessage = error.getMessage();
        if (errorMessage == null) {
            return false;
        }
        
        return errorMessage.contains("TimeoutException") ||
               errorMessage.contains("NetworkException") ||
               errorMessage.contains("NotLeaderForPartitionException") ||
               errorMessage.contains("RecordTooLargeException") == false;
    }
    
    /**
     * 재시도 스케줄링
     */
    private void scheduleRetry(DomainEvent event, int attemptNumber) {
        if (attemptNumber > maxRetryAttempts) {
            log.error("Max retry attempts ({}) exceeded for event: {}", 
                maxRetryAttempts, event.eventType());
            handleMaxRetriesExceeded(event);
            return;
        }
        
        long delay = calculateDelay(attemptNumber);
        
        log.info("Scheduling retry #{} for event: {} after {} ms", 
            attemptNumber, event.eventType(), delay);
        
        scheduledExecutor.schedule(() -> {
            try {
                retryableEventStore.retry(event);
            } catch (Exception e) {
                log.error("Retry #{} failed for event: {}", attemptNumber, event.eventType(), e);
                scheduleRetry(event, attemptNumber + 1);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 지수 백오프를 사용한 재시도 지연 시간 계산
     */
    private long calculateDelay(int attemptNumber) {
        long delay = (long) (initialDelayMs * Math.pow(delayMultiplier, attemptNumber - 1));
        return Math.min(delay, maxDelayMs);
    }
    
    /**
     * 재시도 불가능한 에러 처리
     */
    private void handleNonRetryableError(DomainEvent event, Throwable error) {
        log.error("Non-retryable error for event: {}, Error: {}", 
            event.eventType(), error.getMessage());
        
        // Dead Letter Queue로 이동
        retryableEventStore.moveToDeadLetter(event, error.getMessage());
        
        // 알림 발송 (선택적)
        notifyFailure(event, error);
    }
    
    /**
     * 최대 재시도 횟수 초과 처리
     */
    private void handleMaxRetriesExceeded(DomainEvent event) {
        // Dead Letter Queue로 이동
        retryableEventStore.moveToDeadLetter(event, "Max retries exceeded");
        
        // 알림 발송
        notifyMaxRetriesExceeded(event);
    }
    
    /**
     * 실패 알림 발송
     */
    private void notifyFailure(DomainEvent event, Throwable error) {
        // 실제 구현에서는 모니터링 시스템이나 알림 서비스로 전송
        log.warn("Event publishing failed permanently. Event: {}, Error: {}", 
            event.eventType(), error.getMessage());
    }
    
    /**
     * 최대 재시도 초과 알림
     */
    private void notifyMaxRetriesExceeded(DomainEvent event) {
        // 실제 구현에서는 모니터링 시스템이나 알림 서비스로 전송
        log.warn("Max retries exceeded for event: {}", event.eventType());
    }
    
    /**
     * 리소스 정리
     */
    public void shutdown() {
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}