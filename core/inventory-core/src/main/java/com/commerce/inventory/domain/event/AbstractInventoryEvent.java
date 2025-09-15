package com.commerce.inventory.domain.event;

import com.commerce.common.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class AbstractInventoryEvent implements DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredAt;

    protected AbstractInventoryEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
    
    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return getEventType();
    }

    public abstract String getAggregateId();

    public abstract String getEventType();
}