package com.commerce.inventory.infrastructure.event.kafka;

import com.commerce.common.event.DomainEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이벤트 타입에 따른 Kafka 토픽 결정
 */
@Component
public class KafkaTopicResolver {
    
    @Value("${kafka.topics.prefix:inventory}")
    private String topicPrefix;
    
    @Value("${kafka.topics.default:inventory-events}")
    private String defaultTopic;
    
    private final Map<String, String> topicMappings = new ConcurrentHashMap<>();
    
    public KafkaTopicResolver() {
        // 이벤트 타입별 토픽 매핑 초기화
        initializeTopicMappings();
    }
    
    /**
     * 도메인 이벤트에 대한 Kafka 토픽 결정
     */
    public String resolveTopic(DomainEvent event) {
        String eventType = event.eventType();
        
        // 명시적 매핑이 있는 경우
        if (topicMappings.containsKey(eventType)) {
            return topicMappings.get(eventType);
        }
        
        // 이벤트 타입 기반 토픽 생성
        return generateTopicName(eventType);
    }
    
    /**
     * 이벤트 타입별 토픽 매핑 초기화
     */
    private void initializeTopicMappings() {
        // Stock 관련 이벤트
        topicMappings.put("StockReceivedEvent", "inventory-stock-events");
        topicMappings.put("StockReservedEvent", "inventory-stock-events");
        topicMappings.put("StockDepletedEvent", "inventory-stock-events");
        topicMappings.put("ReservationReleasedEvent", "inventory-stock-events");
        
        // SKU 관련 이벤트
        topicMappings.put("SkuCreatedEvent", "inventory-sku-events");
        topicMappings.put("SkuUpdatedEvent", "inventory-sku-events");
        topicMappings.put("SkuDeletedEvent", "inventory-sku-events");
        
        // Movement 관련 이벤트
        topicMappings.put("StockMovementCreatedEvent", "inventory-movement-events");
    }
    
    /**
     * 이벤트 타입으로부터 토픽 이름 생성
     */
    private String generateTopicName(String eventType) {
        // EventType이 "StockReceivedEvent"인 경우 -> "inventory-stock-received"
        String topicSuffix = eventType
            .replaceAll("Event$", "")
            .replaceAll("([a-z])([A-Z])", "$1-$2")
            .toLowerCase();
        
        return topicPrefix + "-" + topicSuffix;
    }
    
    /**
     * 특정 이벤트 타입에 대한 토픽 매핑 추가
     */
    public void addMapping(String eventType, String topic) {
        topicMappings.put(eventType, topic);
    }
    
    /**
     * 특정 이벤트 타입에 대한 토픽 매핑 제거
     */
    public void removeMapping(String eventType) {
        topicMappings.remove(eventType);
    }
}