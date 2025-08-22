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

    @Pointcut("execution(* com.commerce.product.domain.repository..*.save(..)) && args(aggregate)")
    public void repositorySave(AggregateRoot<?> aggregate) {}

    // aggregate 파라미터는 AspectJ 포인트컷 바인딩에 필요하며, 의도적으로 사용되지 않음
    @AfterReturning(pointcut = "repositorySave(aggregate)", returning = "savedAggregate")
    public void publishDomainEvents(AggregateRoot<?> aggregate, AggregateRoot<?> savedAggregate) {
        if (savedAggregate != null) {
            savedAggregate.pullDomainEvents().forEach(event -> {
                log.debug("Publishing domain event: {}", event);
                eventPublisher.publishEvent(event);
            });
        }
    }
}