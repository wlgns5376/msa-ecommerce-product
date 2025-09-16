package com.commerce.common.event;

import java.util.Collection;

/**
 * 도메인 이벤트 발행을 위한 인터페이스
 */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishEvents(Collection<DomainEvent> events);
}