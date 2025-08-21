package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublicationDelegate {
    
    private final DomainEventPublisher eventPublisher;
    private final DeadLetterQueueService deadLetterQueueService;
    
    @Retryable(
        maxAttemptsExpression = "${event.publication.retry.max-attempts:3}",
        backoff = @Backoff(
            delayExpression = "${event.publication.retry.initial-delay:1000}",
            multiplierExpression = "${event.publication.retry.multiplier:2}",
            maxDelayExpression = "${event.publication.retry.max-delay:5000}"
        ),
        retryFor = {
            IOException.class,
            TimeoutException.class,
            TransientDataAccessException.class
        }
    )
    public void publishWithRetry(DomainEvent event) throws IOException, TimeoutException {
        try {
            eventPublisher.publish(event);
            log.info("Successfully published domain event of type {}", event.getClass().getSimpleName());
        } catch (RuntimeException e) {
            log.error("Failed to publish domain event of type {}: {}. Event: {}", 
                event.getClass().getSimpleName(), e.getMessage(), event, e);
            
            // RuntimeException 내부의 원인이 재시도 가능한 예외인지 확인
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            } else if (cause instanceof TimeoutException timeoutException) {
                throw timeoutException;
            } else if (e instanceof TransientDataAccessException) {
                throw e;
            }
            
            // 그 외의 경우 원래 예외를 그대로 던짐
            throw e;
        }
    }
    
    @Recover
    public void handleFailedEvent(IOException e, DomainEvent event) {
        handleEventFailure(e, event);
    }
    
    @Recover
    public void handleFailedEvent(TimeoutException e, DomainEvent event) {
        handleEventFailure(e, event);
    }
    
    @Recover
    public void handleFailedEvent(TransientDataAccessException e, DomainEvent event) {
        handleEventFailure(e, event);
    }
    
    private void handleEventFailure(Exception e, DomainEvent event) {
        log.error("Failed to publish domain event after all retries. Event type: {}, Event: {}", 
            event.getClass().getSimpleName(), event, e);
        
        // Dead Letter Queue에 실패한 이벤트 저장
        try {
            deadLetterQueueService.storeFailedEvent(event, e);
            log.info("Failed event stored in Dead Letter Queue. Event type: {}", 
                event.getClass().getSimpleName());
        } catch (Exception dlqException) {
            // DLQ 저장도 실패한 경우 - 이는 매우 심각한 상황
            log.error("CRITICAL: Failed to store event in Dead Letter Queue. Event may be lost! " +
                "Event type: {}, Event: {}", event.getClass().getSimpleName(), event, dlqException);
            
            // TODO: 다음 단계 구현 필요
            // 1. 긴급 알림 시스템 호출 (PagerDuty, Slack 등)
            // 2. 로컬 파일 시스템에 임시 저장
            // 3. Circuit Breaker 패턴 적용하여 시스템 보호
            // 4. Fallback 메커니즘 구현 (예: 이벤트를 메모리 큐에 보관)
            
            // 현재는 예외를 다시 던져서 트랜잭션 롤백을 유도
            throw new EventPublicationException(
                "Failed to publish event and store in DLQ", dlqException, event);
        }
    }
}