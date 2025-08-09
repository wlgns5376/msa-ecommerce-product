package com.commerce.common.event;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트를 나타내는 인터페이스
 */
public interface DomainEvent {
    LocalDateTime getOccurredAt();
    default LocalDateTime occurredAt() {
        return getOccurredAt();
    }
    String eventType();
}