package com.commerce.inventory.infrastructure.event.kafka;

/**
 * Kafka 토픽 관리를 위한 Enum
 * 이벤트 타입별로 토픽을 정의하고 매핑
 */
public enum KafkaTopic {
    // Stock 관련 이벤트
    STOCK_EVENTS("inventory-stock-events", 
        "StockReceivedEvent", 
        "StockReservedEvent", 
        "StockDepletedEvent", 
        "ReservationReleasedEvent"),
    
    // SKU 관련 이벤트
    SKU_EVENTS("inventory-sku-events",
        "SkuCreatedEvent",
        "SkuUpdatedEvent",
        "SkuDeletedEvent"),
    
    // Movement 관련 이벤트
    MOVEMENT_EVENTS("inventory-movement-events",
        "StockMovementCreatedEvent");
    
    private final String topicName;
    private final String[] eventTypes;
    
    KafkaTopic(String topicName, String... eventTypes) {
        this.topicName = topicName;
        this.eventTypes = eventTypes;
    }
    
    public String getTopicName() {
        return topicName;
    }
    
    public String[] getEventTypes() {
        return eventTypes;
    }
    
    /**
     * 이벤트 타입으로 해당하는 토픽 찾기
     */
    public static KafkaTopic findByEventType(String eventType) {
        for (KafkaTopic topic : values()) {
            for (String type : topic.eventTypes) {
                if (type.equals(eventType)) {
                    return topic;
                }
            }
        }
        return null;
    }
    
    /**
     * 이벤트 타입이 이 토픽에 속하는지 확인
     */
    public boolean containsEventType(String eventType) {
        for (String type : eventTypes) {
            if (type.equals(eventType)) {
                return true;
            }
        }
        return false;
    }
}