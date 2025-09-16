package com.commerce.inventory.domain.event;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.SkuId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockReservedEventTest {
    
    @Test
    void shouldCreateStockReservedEvent() {
        // given
        SkuId skuId = SkuId.of("SKU001");
        Quantity quantity = Quantity.of(10);
        Reservation reservation = Reservation.create(skuId, quantity, "ORDER001", 900);
        
        // when
        StockReservedEvent event = new StockReservedEvent(skuId, reservation);
        
        // then
        assertThat(event.getSkuId()).isEqualTo(skuId);
        assertThat(event.getReservation()).isEqualTo(reservation);
        assertThat(event.getAggregateId()).isEqualTo(skuId.value());
        assertThat(event.getEventType()).isEqualTo("inventory.stock.reserved");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }
}