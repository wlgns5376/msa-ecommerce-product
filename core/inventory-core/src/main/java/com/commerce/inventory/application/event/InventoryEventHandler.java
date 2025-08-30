package com.commerce.inventory.application.event;

import com.commerce.common.event.DomainEvent;
import com.commerce.common.event.DomainEventPublisher;
import com.commerce.inventory.domain.event.StockReceivedEvent;
import com.commerce.inventory.domain.event.StockReservedEvent;
import com.commerce.inventory.domain.event.ReservationReleasedEvent;
import com.commerce.inventory.domain.event.StockDepletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 재고 도메인 이벤트 핸들러
 * 내부 도메인 이벤트를 수신하여 필요한 처리를 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventHandler {
    
    private final DomainEventPublisher domainEventPublisher;
    
    @EventListener
    @Async
    public void handle(StockReceivedEvent event) {
        log.info("Stock received event: SKU={}, quantity={}, reference={}", 
            event.getSkuId().value(), 
            event.getQuantity().value(), 
            event.getReference());
        
        // 외부 시스템으로 이벤트 발행
        domainEventPublisher.publish(event);
    }
    
    @EventListener
    @Async
    public void handle(StockReservedEvent event) {
        log.info("Stock reserved event: SKU={}, reservationId={}, quantity={}", 
            event.getSkuId().value(),
            event.getReservation().getId().value(),
            event.getReservation().getQuantity().value());
        
        // 외부 시스템으로 이벤트 발행
        domainEventPublisher.publish(event);
    }
    
    @EventListener
    @Async
    public void handle(ReservationReleasedEvent event) {
        log.info("Reservation released event: SKU={}, reservationId={}", 
            event.getSkuId().value(),
            event.getReservationId().value());
        
        // 외부 시스템으로 이벤트 발행
        domainEventPublisher.publish(event);
    }
    
    @EventListener
    @Async
    public void handle(StockDepletedEvent event) {
        log.warn("Stock depleted event: SKU={}", event.getSkuId().value());
        
        // 외부 시스템으로 이벤트 발행
        domainEventPublisher.publish(event);
    }
}