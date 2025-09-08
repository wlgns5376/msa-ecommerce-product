package com.commerce.inventory.infrastructure.event.kafka;

/**
 * Aggregate에 속하는 이벤트를 위한 인터페이스
 */
public interface AggregateEvent {
    String getAggregateId();
    default String getAggregateType() {
        return this.getClass().getSimpleName().replace("Event", "");
    }
}