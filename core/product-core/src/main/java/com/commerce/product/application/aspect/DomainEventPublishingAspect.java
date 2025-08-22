package com.commerce.product.application.aspect;

import com.commerce.product.domain.model.AggregateRoot;
import com.commerce.product.domain.repository.Repository;
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