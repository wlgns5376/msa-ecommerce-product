package com.commerce.inventory.infrastructure.event.serialization;

import com.commerce.common.event.DomainEvent;
import com.commerce.inventory.infrastructure.event.kafka.AggregateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventSerializer 테스트")
class EventSerializerTest {
    
    private EventSerializer eventSerializer;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        eventSerializer = new EventSerializer(objectMapper);
    }
    
    @Test
    @DisplayName("도메인 이벤트를 EventMessage로 직렬화한다")
    void testSerialize() {
        // Given
        TestDomainEvent event = new TestDomainEvent("test-id", "test data");
        
        // When
        EventMessage message = eventSerializer.serialize(event);
        
        // Then
        assertThat(message).isNotNull();
        assertThat(message.getEventId()).isNotNull();
        assertThat(message.getEventType()).isEqualTo("TestDomainEvent");
        assertThat(message.getOccurredAt()).isNotNull();
        assertThat(message.getPayload()).isNotNull();
        assertThat(message.getMetadata()).containsKey("eventClass");
        assertThat(message.getMetadata()).containsKey("timestamp");
        assertThat(message.getVersion()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("AggregateEvent를 직렬화할 때 aggregateId와 aggregateType을 포함한다")
    void testSerializeAggregateEvent() {
        // Given
        TestAggregateEvent event = new TestAggregateEvent("agg-123", "test data");
        
        // When
        EventMessage message = eventSerializer.serialize(event);
        
        // Then
        assertThat(message.getAggregateId()).isEqualTo("agg-123");
        assertThat(message.getAggregateType()).isEqualTo("TestAggregate");
    }
    
    @Test
    @DisplayName("MetadataProvider를 구현한 이벤트는 추가 메타데이터를 포함한다")
    void testSerializeWithMetadataProvider() {
        // Given
        TestEventWithMetadata event = new TestEventWithMetadata("test-id");
        
        // When
        EventMessage message = eventSerializer.serialize(event);
        
        // Then
        assertThat(message.getMetadata()).containsEntry("customKey", "customValue");
        assertThat(message.getMetadata()).containsEntry("userId", "user-123");
    }
    
    @Test
    @DisplayName("EventMessage를 DomainEvent로 역직렬화한다")
    void testDeserialize() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "test-id");
        payload.put("data", "test data");
        payload.put("occurredAt", LocalDateTime.now().toString());
        
        EventMessage message = EventMessage.builder()
            .eventId("event-123")
            .eventType("TestDomainEvent")
            .payload(payload)
            .occurredAt(LocalDateTime.now())
            .build();
        
        // When
        TestDomainEvent event = eventSerializer.deserialize(message, TestDomainEvent.class);
        
        // Then
        assertThat(event).isNotNull();
        assertThat(event.getId()).isEqualTo("test-id");
        assertThat(event.getData()).isEqualTo("test data");
    }
    
    @Test
    @DisplayName("직렬화 실패 시 EventSerializationException을 던진다")
    void testSerializeFailure() {
        // Given
        InvalidEvent event = new InvalidEvent();
        
        // When & Then
        assertThatThrownBy(() -> eventSerializer.serialize(event))
            .isInstanceOf(EventSerializationException.class)
            .hasMessageContaining("Failed to serialize event");
    }
    
    @Test
    @DisplayName("역직렬화 실패 시 EventSerializationException을 던진다")
    void testDeserializeFailure() {
        // Given
        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("invalid", "data");
        
        EventMessage message = EventMessage.builder()
            .eventId("event-123")
            .eventType("InvalidEvent")
            .payload(invalidPayload)
            .build();
        
        // When & Then
        assertThatThrownBy(() -> eventSerializer.deserialize(message, TestDomainEvent.class))
            .isInstanceOf(EventSerializationException.class)
            .hasMessageContaining("Failed to deserialize event message");
    }
    
    // Test event classes
    static class TestDomainEvent implements DomainEvent {
        private String id;
        private String data;
        private LocalDateTime occurredAt;
        
        public TestDomainEvent() {
            this.occurredAt = LocalDateTime.now();
        }
        
        public TestDomainEvent(String id, String data) {
            this.id = id;
            this.data = data;
            this.occurredAt = LocalDateTime.now();
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
        
        @Override
        public String eventType() {
            return "TestDomainEvent";
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getData() {
            return data;
        }
        
        public void setData(String data) {
            this.data = data;
        }
        
        public void setOccurredAt(LocalDateTime occurredAt) {
            this.occurredAt = occurredAt;
        }
    }
    
    static class TestAggregateEvent extends TestDomainEvent implements AggregateEvent {
        private final String aggregateId;
        
        public TestAggregateEvent(String aggregateId, String data) {
            super(aggregateId, data);
            this.aggregateId = aggregateId;
        }
        
        @Override
        public String getAggregateId() {
            return aggregateId;
        }
        
        @Override
        public String getAggregateType() {
            return "TestAggregate";
        }
    }
    
    static class TestEventWithMetadata extends TestDomainEvent implements EventSerializer.MetadataProvider {
        public TestEventWithMetadata(String id) {
            super(id, "test");
        }
        
        @Override
        public Map<String, String> getMetadata() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("customKey", "customValue");
            metadata.put("userId", "user-123");
            return metadata;
        }
    }
    
    static class InvalidEvent implements DomainEvent {
        @Override
        public LocalDateTime getOccurredAt() {
            throw new RuntimeException("Invalid event");
        }
        
        @Override
        public String eventType() {
            return "InvalidEvent";
        }
    }
}