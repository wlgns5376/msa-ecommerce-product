package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class AbstractDomainEvent implements DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredAt;

    protected AbstractDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return getEventType();
    }

    public abstract String getAggregateId();

    public abstract String getEventType();
}