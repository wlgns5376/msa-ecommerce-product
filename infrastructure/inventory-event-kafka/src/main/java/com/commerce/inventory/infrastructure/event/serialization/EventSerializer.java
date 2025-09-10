package com.commerce.inventory.infrastructure.event.serialization;

import com.commerce.common.event.DomainEvent;
import com.commerce.inventory.infrastructure.event.kafka.AggregateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 도메인 이벤트를 Kafka 메시지로 변환하는 직렬화 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventSerializer {
    
    private final ObjectMapper objectMapper;
    
    /**
     * DomainEvent를 EventMessage로 직렬화
     */
    public EventMessage serialize(DomainEvent event) {
        try {
            Map<String, Object> payload = convertToMap(event);
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            return EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(event.eventType())
                .occurredAt(Instant.from(event.getOccurredAt()))
                .payload(payloadJson)
                .metadata(extractMetadata(event))
                .aggregateId(extractAggregateId(event))
                .aggregateType(extractAggregateType(event))
                .version(1)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to serialize event: {}", event, e);
            throw new EventSerializationException("Failed to serialize event", e);
        }
    }
    
    /**
     * EventMessage를 특정 타입의 DomainEvent로 역직렬화
     */
    public <T extends DomainEvent> T deserialize(EventMessage message, Class<T> eventClass) {
        try {
            String payloadStr = message.getPayload();
            if (payloadStr == null || payloadStr.isEmpty()) {
                throw new EventSerializationException("Empty payload");
            }
            return objectMapper.readValue(payloadStr, eventClass);
        } catch (Exception e) {
            log.error("Failed to deserialize event message: {}", message, e);
            throw new EventSerializationException("Failed to deserialize event message", e);
        }
    }
    
    private Map<String, Object> convertToMap(DomainEvent event) {
        try {
            // Convert event object to Map
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(event, Map.class);
            return map;
        } catch (Exception e) {
            log.error("Failed to convert event to map: {}", event, e);
            throw new EventSerializationException("Failed to convert event to map", e);
        }
    }
    
    private Map<String, String> extractMetadata(DomainEvent event) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("eventClass", event.getClass().getName());
        metadata.put("timestamp", event.getOccurredAt().toString());
        
        // Add custom metadata if event implements MetadataProvider
        if (event instanceof MetadataProvider) {
            metadata.putAll(((MetadataProvider) event).getMetadata());
        }
        
        return metadata;
    }
    
    private String extractAggregateId(DomainEvent event) {
        if (event instanceof AggregateEvent) {
            return ((AggregateEvent) event).getAggregateId();
        }
        return null;
    }
    
    private String extractAggregateType(DomainEvent event) {
        if (event instanceof AggregateEvent) {
            return ((AggregateEvent) event).getAggregateType();
        }
        return null;
    }
    
    /**
     * Interface for events that provide additional metadata
     */
    public interface MetadataProvider {
        Map<String, String> getMetadata();
    }
}