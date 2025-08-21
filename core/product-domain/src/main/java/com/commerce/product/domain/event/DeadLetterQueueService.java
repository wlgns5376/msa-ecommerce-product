package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;

public interface DeadLetterQueueService {
    
    void storeFailedEvent(DomainEvent event, Exception failureReason);
    
    void processDeadLetterQueue();
}