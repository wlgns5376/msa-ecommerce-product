package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;

public class EventPublicationException extends RuntimeException {
    
    private final DomainEvent failedEvent;
    
    public EventPublicationException(String message, Throwable cause, DomainEvent failedEvent) {
        super(message, cause);
        this.failedEvent = failedEvent;
    }
    
    public DomainEvent getFailedEvent() {
        return failedEvent;
    }
}