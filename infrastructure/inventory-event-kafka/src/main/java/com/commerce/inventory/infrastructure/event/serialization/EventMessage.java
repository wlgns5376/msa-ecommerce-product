package com.commerce.inventory.infrastructure.event.serialization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka로 전송될 이벤트 메시지 포맷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {
    
    private String eventId;
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private Instant occurredAt;
    private String payload;  // JSON string으로 저장
    private Map<String, String> metadata;
    private Integer version;
    
    public static EventMessage create(String eventType, String payload) {
        return EventMessage.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType)
            .payload(payload)
            .occurredAt(Instant.now())
            .version(1)
            .build();
    }
}