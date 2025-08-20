package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDomainEventHandler {
    
    private final DomainEventPublisher eventPublisher;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainEvent(DomainEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Failed to publish domain event of type {}: {}. Event: {}", 
                event.getClass().getSimpleName(), e.getMessage(), event, e);
            // TODO: 실패한 이벤트를 Dead Letter Queue에 보내는 등의 추가적인 오류 처리 로직을 고려해야 합니다.
        }
    }
}
