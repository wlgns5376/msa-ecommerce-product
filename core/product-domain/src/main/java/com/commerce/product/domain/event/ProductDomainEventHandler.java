package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDomainEventHandler {
    
    private final EventPublicationDelegate delegate;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainEvent(DomainEvent event) {
        delegate.publishWithRetry(event);
    }
}