package com.commerce.inventory.infrastructure.event.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EventDeserializerTest {

    private EventDeserializer deserializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        deserializer = new EventDeserializer();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void shouldDeserializeValidEventMessage() throws Exception {
        // Given
        EventMessage originalMessage = EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("StockReservedEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload("{\"skuId\":\"SKU-001\",\"quantity\":10}")
                .occurredAt(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(originalMessage);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        // When
        EventMessage deserializedMessage = deserializer.deserialize("test-topic", data);

        // Then
        assertThat(deserializedMessage).isNotNull();
        assertThat(deserializedMessage.getEventId()).isEqualTo(originalMessage.getEventId());
        assertThat(deserializedMessage.getEventType()).isEqualTo(originalMessage.getEventType());
        assertThat(deserializedMessage.getAggregateId()).isEqualTo(originalMessage.getAggregateId());
        assertThat(deserializedMessage.getAggregateType()).isEqualTo(originalMessage.getAggregateType());
        assertThat(deserializedMessage.getPayload()).isEqualTo(originalMessage.getPayload());
    }

    @Test
    void shouldReturnNullForNullData() {
        // When
        EventMessage result = deserializer.deserialize("test-topic", null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForEmptyData() {
        // When
        EventMessage result = deserializer.deserialize("test-topic", new byte[0]);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowSerializationExceptionForInvalidJson() {
        // Given
        byte[] invalidData = "invalid json".getBytes(StandardCharsets.UTF_8);

        // When/Then
        assertThatThrownBy(() -> deserializer.deserialize("test-topic", invalidData))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Failed to deserialize event from topic test-topic");
    }

    @Test
    void shouldDeserializeEventWithNullPayload() throws Exception {
        // Given
        EventMessage originalMessage = EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("StockDepletedEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload(null)
                .occurredAt(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(originalMessage);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        // When
        EventMessage deserializedMessage = deserializer.deserialize("test-topic", data);

        // Then
        assertThat(deserializedMessage).isNotNull();
        assertThat(deserializedMessage.getPayload()).isNull();
    }

    @Test
    void shouldDeserializeComplexPayload() throws Exception {
        // Given
        String complexPayload = "{\"skuId\":\"SKU-001\",\"quantities\":[10,20,30],\"metadata\":{\"source\":\"API\",\"userId\":\"user123\"}}";
        EventMessage originalMessage = EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ComplexEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload(complexPayload)
                .occurredAt(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(originalMessage);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        // When
        EventMessage deserializedMessage = deserializer.deserialize("test-topic", data);

        // Then
        assertThat(deserializedMessage).isNotNull();
        assertThat(deserializedMessage.getPayload()).isEqualTo(complexPayload);
    }
}