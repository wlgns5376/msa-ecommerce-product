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
    
    private final Map<String, String> customTopicMappings = new ConcurrentHashMap<>();
    
    /**
     * 도메인 이벤트에 대한 Kafka 토픽 결정
     */
    public String resolveTopic(DomainEvent event) {
        String eventType = event.eventType();
        
        // 커스텀 매핑이 있는 경우 우선 사용
        if (customTopicMappings.containsKey(eventType)) {
            return customTopicMappings.get(eventType);
        }
        
        // Enum 기반 매핑 확인
        KafkaTopic kafkaTopic = KafkaTopic.findByEventType(eventType);
        if (kafkaTopic != null) {
            return kafkaTopic.getTopicName();
        }
        
        // 이벤트 타입 기반 토픽 생성
        return generateTopicName(eventType);
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
     * 특정 이벤트 타입에 대한 커스텀 토픽 매핑 추가
     * Enum에 정의되지 않은 이벤트 타입에 대한 동적 매핑을 위해 사용
     */
    public void addCustomMapping(String eventType, String topic) {
        customTopicMappings.put(eventType, topic);
    }
    
    /**
     * 특정 이벤트 타입에 대한 커스텀 토픽 매핑 제거
     */
    public void removeCustomMapping(String eventType) {
        customTopicMappings.remove(eventType);
    }
}