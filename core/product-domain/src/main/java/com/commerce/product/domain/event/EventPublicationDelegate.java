package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublicationDelegate {
    
    private final DomainEventPublisher eventPublisher;
    
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000),
        retryFor = { RuntimeException.class },
        noRetryFor = { IllegalArgumentException.class, IllegalStateException.class }
    )
    public void publishWithRetry(DomainEvent event) {
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
    public void handleFailedEvent(RuntimeException e, DomainEvent event) {
        log.error("Failed to publish domain event after all retries. Event type: {}, Event: {}", 
            event.getClass().getSimpleName(), event, e);
        
        // CRITICAL TODO: Dead Letter Queue 구현 필수 (프로덕션 배포 전 반드시 구현)
        // WARNING: 현재는 이벤트 발행 실패 시 유실되는 심각한 문제가 있습니다.
        // 이벤트 유실은 데이터 불일치를 야기할 수 있는 매우 중요한 이슈입니다.
        // 
        // 구현 요구사항:
        // 1. 실패한 이벤트를 별도의 저장소(Redis, DB 등)에 저장
        // 2. 주기적으로 실패한 이벤트를 재처리하는 배치 작업 구현
        // 3. 모니터링 및 알림 시스템 연동
        // 4. 실패 원인 분석을 위한 메타데이터 저장 (실패 시각, 재시도 횟수, 에러 메시지 등)
        // 
        // 현재는 로그만 남기고 정상 처리로 간주하여 시스템이 계속 동작하도록 함
        // 이는 임시 조치이며, 프로덕션 환경에서는 절대 허용되지 않습니다.
    }
}