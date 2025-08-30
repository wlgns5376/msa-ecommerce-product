package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;
import com.commerce.common.event.DomainEvent;
import com.commerce.inventory.domain.event.ReservationReleasedEvent;
import com.commerce.inventory.domain.event.StockDepletedEvent;
import com.commerce.inventory.domain.event.StockReceivedEvent;
import com.commerce.inventory.domain.event.StockReservedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryEventTest {
    
    private SkuId skuId;
    private Inventory inventory;
    
    @BeforeEach
    void setUp() {
        skuId = SkuId.of("SKU001");
        inventory = Inventory.createWithInitialStock(skuId, Quantity.of(100));
    }
    
    @Test
    void shouldRaiseStockReceivedEventWhenReceivingStock() {
        // given
        Quantity receiveQuantity = Quantity.of(50);
        String reference = "PO-2023-001";
        
        // when
        inventory.receive(receiveQuantity, reference);
        
        // then
        List<DomainEvent> events = inventory.getDomainEvents();
        assertThat(events).hasSize(1);
        
        StockReceivedEvent event = (StockReceivedEvent) events.get(0);
        assertThat(event.getSkuId()).isEqualTo(skuId);
        assertThat(event.getQuantity()).isEqualTo(receiveQuantity);
        assertThat(event.getReference()).isEqualTo(reference);
    }
    
    @Test
    void shouldRaiseStockReservedEventWhenReservingStock() {
        // given
        Quantity reserveQuantity = Quantity.of(20);
        String orderId = "ORDER001";
        int ttlSeconds = 900;
        
        // when
        Reservation reservation = inventory.reserve(reserveQuantity, orderId, ttlSeconds);
        
        // then
        List<DomainEvent> events = inventory.getDomainEvents();
        assertThat(events).hasSize(1);
        
        StockReservedEvent event = (StockReservedEvent) events.get(0);
        assertThat(event.getSkuId()).isEqualTo(skuId);
        assertThat(event.getReservation()).isEqualTo(reservation);
    }
    
    @Test
    void shouldRaiseStockDepletedEventWhenReservingAllAvailableStock() {
        // given
        inventory = Inventory.createWithInitialStock(skuId, Quantity.of(10));
        Quantity reserveQuantity = Quantity.of(10);
        String orderId = "ORDER001";
        int ttlSeconds = 900;
        
        // when
        inventory.reserve(reserveQuantity, orderId, ttlSeconds);
        
        // then
        List<DomainEvent> events = inventory.getDomainEvents();
        assertThat(events).hasSize(2);
        
        assertThat(events.get(0)).isInstanceOf(StockReservedEvent.class);
        assertThat(events.get(1)).isInstanceOf(StockDepletedEvent.class);
        
        StockDepletedEvent depletedEvent = (StockDepletedEvent) events.get(1);
        assertThat(depletedEvent.getSkuId()).isEqualTo(skuId);
    }
    
    @Test
    void shouldRaiseReservationReleasedEventWhenReleasingReservation() {
        // given
        Quantity reserveQuantity = Quantity.of(20);
        String orderId = "ORDER001";
        Reservation reservation = inventory.reserve(reserveQuantity, orderId, 900);
        inventory.pullDomainEvents(); // 이전 이벤트 제거
        
        // when
        inventory.releaseReservedQuantity(reserveQuantity, reservation.getId());
        
        // then
        List<DomainEvent> events = inventory.getDomainEvents();
        assertThat(events).hasSize(1);
        
        ReservationReleasedEvent event = (ReservationReleasedEvent) events.get(0);
        assertThat(event.getSkuId()).isEqualTo(skuId);
        assertThat(event.getReservationId()).isEqualTo(reservation.getId());
    }
    
    @Test
    void shouldClearEventsAfterPulling() {
        // given
        inventory.receive(Quantity.of(50), "PO-001");
        
        // when
        List<DomainEvent> pulledEvents = inventory.pullDomainEvents();
        List<DomainEvent> remainingEvents = inventory.getDomainEvents();
        
        // then
        assertThat(pulledEvents).hasSize(1);
        assertThat(remainingEvents).isEmpty();
    }
}