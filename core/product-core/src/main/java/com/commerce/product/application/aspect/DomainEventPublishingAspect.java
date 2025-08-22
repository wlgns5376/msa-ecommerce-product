package com.commerce.product.application.aspect;

import com.commerce.product.domain.model.AggregateRoot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublishingAspect {

    private final ApplicationEventPublisher eventPublisher;

    @Pointcut("execution(* com.commerce.product.domain.repository..*.save(..))")
    public void repositorySave() {}

    @AfterReturning(pointcut = "repositorySave()", returning = "savedAggregate")
    public void publishDomainEvents(AggregateRoot<?> savedAggregate) {
        if (savedAggregate != null) {
            // 먼저 이벤트 목록을 가져옵니다 (읽기 전용)
            var events = savedAggregate.getDomainEvents();
            
            try {
                // 모든 이벤트를 발행합니다
                for (var event : events) {
                    log.debug("Publishing domain event: {}", event);
                    eventPublisher.publishEvent(event);
                }
                
                // 모든 이벤트가 성공적으로 발행된 후에만 이벤트를 클리어합니다
                savedAggregate.pullDomainEvents();
            } catch (Exception e) {
                // 이벤트 발행 실패 시 로그를 남기고 예외를 전파합니다
                log.error("Failed to publish domain events. Events will remain in aggregate for retry.", e);
                throw e;
            }
        }
    }
}