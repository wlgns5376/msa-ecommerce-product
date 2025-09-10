package com.commerce.inventory.infrastructure.event.consumer;

import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import com.commerce.inventory.infrastructure.event.handler.EventHandler;
import com.commerce.inventory.infrastructure.event.handler.EventHandlerRegistry;
import com.commerce.inventory.infrastructure.event.idempotency.IdempotencyService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventConsumerTest {

    @Mock
    private EventHandlerRegistry handlerRegistry;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private EventHandler eventHandler;

    @Mock
    private Acknowledgment acknowledgment;

    private KafkaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new KafkaEventConsumer(handlerRegistry, idempotencyService);
    }

    @Test
    void shouldConsumeEventSuccessfully() {
        // Given
        String eventId = UUID.randomUUID().toString();
        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("StockReservedEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload("{\"skuId\":\"SKU-001\",\"quantity\":10}")
                .occurredAt(Instant.now())
                .build();

        ConsumerRecord<String, EventMessage> record = new ConsumerRecord<>(
                "inventory-events", 0, 0L, "SKU-001", eventMessage
        );

        when(idempotencyService.isProcessed(eventId)).thenReturn(false);
        when(handlerRegistry.getHandler("StockReservedEvent")).thenReturn(eventHandler);
        when(eventHandler.handle(any())).thenReturn(CompletableFuture.completedFuture(null));

        // When
        consumer.consume(record, acknowledgment);

        // Then
        verify(idempotencyService).isProcessed(eventId);
        verify(handlerRegistry).getHandler("StockReservedEvent");
        verify(eventHandler).handle(eventMessage);
        verify(idempotencyService).markAsProcessed(eventId);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldSkipAlreadyProcessedEvent() {
        // Given
        String eventId = UUID.randomUUID().toString();
        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("StockReservedEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload("{\"skuId\":\"SKU-001\",\"quantity\":10}")
                .occurredAt(Instant.now())
                .build();

        ConsumerRecord<String, EventMessage> record = new ConsumerRecord<>(
                "inventory-events", 0, 0L, "SKU-001", eventMessage
        );

        when(idempotencyService.isProcessed(eventId)).thenReturn(true);

        // When
        consumer.consume(record, acknowledgment);

        // Then
        verify(idempotencyService).isProcessed(eventId);
        verify(handlerRegistry, never()).getHandler(any());
        verify(eventHandler, never()).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleEventHandlerException() {
        // Given
        String eventId = UUID.randomUUID().toString();
        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("StockReservedEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload("{\"skuId\":\"SKU-001\",\"quantity\":10}")
                .occurredAt(Instant.now())
                .build();

        ConsumerRecord<String, EventMessage> record = new ConsumerRecord<>(
                "inventory-events", 0, 0L, "SKU-001", eventMessage
        );

        when(idempotencyService.isProcessed(eventId)).thenReturn(false);
        when(handlerRegistry.getHandler("StockReservedEvent")).thenReturn(eventHandler);
        when(eventHandler.handle(any())).thenThrow(new RuntimeException("Handler error"));

        // When
        consumer.consume(record, acknowledgment);

        // Then
        verify(idempotencyService).isProcessed(eventId);
        verify(handlerRegistry).getHandler("StockReservedEvent");
        verify(eventHandler).handle(eventMessage);
        verify(idempotencyService, never()).markAsProcessed(eventId);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void shouldHandleNullEventHandler() {
        // Given
        String eventId = UUID.randomUUID().toString();
        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("UnknownEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload("{}")
                .occurredAt(Instant.now())
                .build();

        ConsumerRecord<String, EventMessage> record = new ConsumerRecord<>(
                "inventory-events", 0, 0L, "SKU-001", eventMessage
        );

        when(idempotencyService.isProcessed(eventId)).thenReturn(false);
        when(handlerRegistry.getHandler("UnknownEvent")).thenReturn(null);

        // When
        consumer.consume(record, acknowledgment);

        // Then
        verify(idempotencyService).isProcessed(eventId);
        verify(handlerRegistry).getHandler("UnknownEvent");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldConsumeEventInBatch() {
        // Given
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();
        
        EventMessage eventMessage1 = EventMessage.builder()
                .eventId(eventId1)
                .eventType("StockReservedEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload("{\"skuId\":\"SKU-001\",\"quantity\":10}")
                .occurredAt(Instant.now())
                .build();

        EventMessage eventMessage2 = EventMessage.builder()
                .eventId(eventId2)
                .eventType("StockReceivedEvent")
                .aggregateId("SKU-002")
                .aggregateType("SKU")
                .payload("{\"skuId\":\"SKU-002\",\"quantity\":20}")
                .occurredAt(Instant.now())
                .build();

        ConsumerRecord<String, EventMessage> record1 = new ConsumerRecord<>(
                "inventory-events", 0, 0L, "SKU-001", eventMessage1
        );
        ConsumerRecord<String, EventMessage> record2 = new ConsumerRecord<>(
                "inventory-events", 0, 1L, "SKU-002", eventMessage2
        );

        when(idempotencyService.isProcessed(eventId1)).thenReturn(false);
        when(idempotencyService.isProcessed(eventId2)).thenReturn(false);
        when(handlerRegistry.getHandler(any())).thenReturn(eventHandler);
        when(eventHandler.handle(any())).thenReturn(CompletableFuture.completedFuture(null));

        // When
        consumer.consume(record1, acknowledgment);
        consumer.consume(record2, acknowledgment);

        // Then
        verify(idempotencyService, times(2)).isProcessed(any());
        verify(handlerRegistry, times(2)).getHandler(any());
        verify(eventHandler, times(2)).handle(any());
        verify(idempotencyService, times(2)).markAsProcessed(any());
        verify(acknowledgment, times(2)).acknowledge();
    }
}