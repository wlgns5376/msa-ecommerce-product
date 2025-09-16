package com.commerce.inventory.domain.event;

import com.commerce.inventory.domain.model.SkuId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockDepletedEventTest {
    
    @Test
    void shouldCreateStockDepletedEvent() {
        // given
        SkuId skuId = SkuId.of("SKU001");
        
        // when
        StockDepletedEvent event = new StockDepletedEvent(skuId);
        
        // then
        assertThat(event.getSkuId()).isEqualTo(skuId);
        assertThat(event.getAggregateId()).isEqualTo(skuId.value());
        assertThat(event.getEventType()).isEqualTo("inventory.stock.depleted");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }
}