package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductDomainEventHandler {
    
    private final DomainEventPublisher eventPublisher;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainEvent(DomainEvent event) {
        eventPublisher.publish(event);
    }
}