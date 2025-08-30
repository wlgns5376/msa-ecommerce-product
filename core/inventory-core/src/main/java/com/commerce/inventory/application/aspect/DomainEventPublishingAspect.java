package com.commerce.inventory.application.aspect;

import com.commerce.common.domain.model.AggregateRoot;
import com.commerce.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 도메인 이벤트 발행을 위한 AOP Aspect
 * Repository save 메서드 실행 후 Aggregate의 도메인 이벤트를 자동으로 발행합니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DomainEventPublishingAspect {
    
    private final ApplicationEventPublisher eventPublisher;
    
    @AfterReturning(
        pointcut = "execution(* com.commerce.inventory.application.service.port.out.Save*.save(..)) && args(aggregate)",
        argNames = "aggregate"
    )
    public void publishEvents(AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.pullDomainEvents();
        
        events.forEach(event -> {
            log.debug("Publishing domain event: {}", event.getClass().getSimpleName());
            eventPublisher.publishEvent(event);
        });
    }
    
    @AfterReturning(
        pointcut = "execution(* com.commerce.inventory.application.service.port.out.Save*.saveAll(..)) && args(aggregates)",
        argNames = "aggregates"
    )
    public void publishEventsForAll(List<? extends AggregateRoot<?>> aggregates) {
        aggregates.forEach(this::publishEvents);
    }
}