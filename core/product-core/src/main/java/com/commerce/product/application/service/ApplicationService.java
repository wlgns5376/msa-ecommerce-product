package com.commerce.product.application.service;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.model.AggregateRoot;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 애플리케이션 서비스의 기본 클래스
 * 트랜잭션 관리와 도메인 이벤트 발행을 담당합니다.
 */
@RequiredArgsConstructor
@Transactional
public abstract class ApplicationService {
    
    private final DomainEventPublisher domainEventPublisher;
    
    /**
     * 애그리게이트의 도메인 이벤트를 발행합니다.
     */
    protected void publishDomainEvents(AggregateRoot<?> aggregateRoot) {
        List<DomainEvent> events = aggregateRoot.clearDomainEvents();
        events.forEach(domainEventPublisher::publish);
    }
    
    /**
     * 여러 애그리게이트의 도메인 이벤트를 발행합니다.
     */
    protected void publishDomainEvents(List<? extends AggregateRoot<?>> aggregateRoots) {
        aggregateRoots.forEach(this::publishDomainEvents);
    }
}