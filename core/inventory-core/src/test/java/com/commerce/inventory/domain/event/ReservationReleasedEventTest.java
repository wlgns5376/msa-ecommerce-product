package com.commerce.inventory.domain.event;

import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.SkuId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationReleasedEventTest {
    
    @Test
    void shouldCreateReservationReleasedEvent() {
        // given
        SkuId skuId = SkuId.of("SKU001");
        ReservationId reservationId = ReservationId.generate();
        
        // when
        ReservationReleasedEvent event = new ReservationReleasedEvent(skuId, reservationId);
        
        // then
        assertThat(event.getSkuId()).isEqualTo(skuId);
        assertThat(event.getReservationId()).isEqualTo(reservationId);
        assertThat(event.getAggregateId()).isEqualTo(skuId.value());
        assertThat(event.getEventType()).isEqualTo("inventory.reservation.released");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }
}