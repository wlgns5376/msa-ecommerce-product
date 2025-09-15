package com.commerce.inventory.infrastructure.event.serialization;

/**
 * 이벤트 직렬화 중 발생하는 예외
 */
public class EventSerializationException extends RuntimeException {
    
    public EventSerializationException(String message) {
        super(message);
    }
    
    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}