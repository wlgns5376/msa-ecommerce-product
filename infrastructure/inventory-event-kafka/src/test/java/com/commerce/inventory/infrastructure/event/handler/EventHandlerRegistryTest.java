package com.commerce.inventory.infrastructure.event.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventHandlerRegistryTest {

    private EventHandlerRegistry registry;
    private EventHandler mockHandler1;
    private EventHandler mockHandler2;

    @BeforeEach
    void setUp() {
        registry = new EventHandlerRegistry();
        mockHandler1 = mock(EventHandler.class);
        mockHandler2 = mock(EventHandler.class);
    }

    @Test
    void shouldRegisterEventHandler() {
        // When
        registry.register("StockReservedEvent", mockHandler1);

        // Then
        EventHandler handler = registry.getHandler("StockReservedEvent");
        assertThat(handler).isNotNull();
        assertThat(handler).isSameAs(mockHandler1);
    }

    @Test
    void shouldRegisterMultipleHandlers() {
        // When
        registry.register("StockReservedEvent", mockHandler1);
        registry.register("StockReceivedEvent", mockHandler2);

        // Then
        assertThat(registry.getHandler("StockReservedEvent")).isSameAs(mockHandler1);
        assertThat(registry.getHandler("StockReceivedEvent")).isSameAs(mockHandler2);
    }

    @Test
    void shouldOverrideExistingHandler() {
        // When
        registry.register("StockReservedEvent", mockHandler1);
        registry.register("StockReservedEvent", mockHandler2);

        // Then
        EventHandler handler = registry.getHandler("StockReservedEvent");
        assertThat(handler).isSameAs(mockHandler2);
    }

    @Test
    void shouldReturnNullForUnregisteredEventType() {
        // When
        EventHandler handler = registry.getHandler("UnknownEvent");

        // Then
        assertThat(handler).isNull();
    }

    @Test
    void shouldUnregisterHandler() {
        // Given
        registry.register("StockReservedEvent", mockHandler1);

        // When
        registry.unregister("StockReservedEvent");

        // Then
        EventHandler handler = registry.getHandler("StockReservedEvent");
        assertThat(handler).isNull();
    }

    @Test
    void shouldGetAllRegisteredEventTypes() {
        // When
        registry.register("StockReservedEvent", mockHandler1);
        registry.register("StockReceivedEvent", mockHandler2);

        // Then
        assertThat(registry.getRegisteredEventTypes())
                .containsExactlyInAnyOrder("StockReservedEvent", "StockReceivedEvent");
    }

    @Test
    void shouldClearAllHandlers() {
        // Given
        registry.register("StockReservedEvent", mockHandler1);
        registry.register("StockReceivedEvent", mockHandler2);

        // When
        registry.clear();

        // Then
        assertThat(registry.getHandler("StockReservedEvent")).isNull();
        assertThat(registry.getHandler("StockReceivedEvent")).isNull();
        assertThat(registry.getRegisteredEventTypes()).isEmpty();
    }
}