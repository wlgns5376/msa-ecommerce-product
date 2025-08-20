package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDomainEventHandler {
    
    private final DomainEventPublisher eventPublisher;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainEvent(DomainEvent event) {
        publishEventWithRetry(event);
    }
    
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000),
        include = { Exception.class }
    )
    public void publishEventWithRetry(DomainEvent event) {
        try {
            eventPublisher.publish(event);
            log.info("Successfully published domain event of type {}", event.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to publish domain event of type {}: {}. Event: {}", 
                event.getClass().getSimpleName(), e.getMessage(), event, e);
            throw e;
        }
    }
    
    @Recover
    public void handleFailedEvent(Exception e, DomainEvent event) {
        log.error("Failed to publish domain event after all retries. Event type: {}, Event: {}", 
            event.getClass().getSimpleName(), event, e);
        
        // TODO: Dead Letter Queue 구현
        // 1. 실패한 이벤트를 별도의 저장소(Redis, DB 등)에 저장
        // 2. 주기적으로 실패한 이벤트를 재처리하는 배치 작업 구현
        // 3. 모니터링 및 알림 시스템 연동
        
        // 현재는 로그만 남기고 정상 처리로 간주하여 시스템이 계속 동작하도록 함
        // 실제 프로덕션에서는 Dead Letter Queue 구현이 필수
    }
}