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
        try {
            delegate.publishWithRetry(event);
        } catch (Exception e) {
            // TransactionalEventListener는 checked exception을 던질 수 없으므로
            // RuntimeException으로 감싸서 던짐
            throw new RuntimeException("Failed to publish domain event", e);
        }
    }
}