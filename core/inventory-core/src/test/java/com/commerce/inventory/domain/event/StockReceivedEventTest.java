package com.commerce.inventory.domain.event;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.SkuId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockReceivedEventTest {
    
    @Test
    void shouldCreateStockReceivedEvent() {
        // given
        SkuId skuId = SkuId.of("SKU001");
        Quantity quantity = Quantity.of(100);
        String reference = "PO-2023-001";
        
        // when
        StockReceivedEvent event = new StockReceivedEvent(skuId, quantity, reference);
        
        // then
        assertThat(event.getSkuId()).isEqualTo(skuId);
        assertThat(event.getQuantity()).isEqualTo(quantity);
        assertThat(event.getReference()).isEqualTo(reference);
        assertThat(event.getAggregateId()).isEqualTo(skuId.value());
        assertThat(event.getEventType()).isEqualTo("inventory.stock.received");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }
}