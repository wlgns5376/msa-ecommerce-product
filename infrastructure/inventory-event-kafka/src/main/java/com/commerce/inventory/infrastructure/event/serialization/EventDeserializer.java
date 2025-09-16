package com.commerce.inventory.infrastructure.event.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * 이벤트 메시지 디시리얼라이저
 */
public class EventDeserializer implements Deserializer<EventMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(EventDeserializer.class);
    
    private final ObjectMapper objectMapper;
    
    public EventDeserializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Override
    public EventMessage deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            logger.warn("Received null or empty data from topic: {}", topic);
            return null;
        }
        
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            EventMessage eventMessage = objectMapper.readValue(json, EventMessage.class);
            
            logger.debug("Successfully deserialized event: eventId={}, eventType={}", 
                       eventMessage.getEventId(), eventMessage.getEventType());
            
            return eventMessage;
        } catch (Exception e) {
            String errorMessage = String.format(
                "Failed to deserialize event from topic %s", topic
            );
            logger.error(errorMessage, e);
            throw new SerializationException(errorMessage, e);
        }
    }
}