package com.commerce.inventory.infrastructure.event.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KafkaTopic Enum 테스트")
class KafkaTopicTest {
    
    @ParameterizedTest
    @DisplayName("이벤트 타입으로 올바른 토픽을 찾는다")
    @CsvSource({
        "StockReceivedEvent, STOCK_EVENTS, inventory-stock-events",
        "StockReservedEvent, STOCK_EVENTS, inventory-stock-events",
        "SkuCreatedEvent, SKU_EVENTS, inventory-sku-events",
        "SkuUpdatedEvent, SKU_EVENTS, inventory-sku-events",
        "StockMovementCreatedEvent, MOVEMENT_EVENTS, inventory-movement-events"
    })
    void testFindByEventType(String eventType, String expectedTopicName, String expectedTopicValue) {
        // When
        KafkaTopic topic = KafkaTopic.findByEventType(eventType);
        
        // Then
        assertThat(topic).isNotNull();
        assertThat(topic.name()).isEqualTo(expectedTopicName);
        assertThat(topic.getTopicName()).isEqualTo(expectedTopicValue);
    }
    
    @Test
    @DisplayName("존재하지 않는 이벤트 타입에 대해 null을 반환한다")
    void testFindByEventType_NotFound() {
        // When
        KafkaTopic topic = KafkaTopic.findByEventType("UnknownEvent");
        
        // Then
        assertThat(topic).isNull();
    }
    
    @ParameterizedTest
    @DisplayName("토픽에 이벤트 타입이 포함되어 있는지 확인한다")
    @CsvSource({
        "STOCK_EVENTS, StockReceivedEvent, true",
        "STOCK_EVENTS, SkuCreatedEvent, false",
        "SKU_EVENTS, SkuUpdatedEvent, true",
        "SKU_EVENTS, StockMovementCreatedEvent, false",
        "MOVEMENT_EVENTS, StockMovementCreatedEvent, true",
        "MOVEMENT_EVENTS, StockReceivedEvent, false"
    })
    void testContainsEventType(String topicName, String eventType, boolean expected) {
        // Given
        KafkaTopic topic = KafkaTopic.valueOf(topicName);
        
        // When
        boolean contains = topic.containsEventType(eventType);
        
        // Then
        assertThat(contains).isEqualTo(expected);
    }
    
    @Test
    @DisplayName("모든 토픽이 고유한 이름을 가진다")
    void testUniqueTopicNames() {
        // Given
        KafkaTopic[] topics = KafkaTopic.values();
        
        // When & Then
        for (int i = 0; i < topics.length; i++) {
            for (int j = i + 1; j < topics.length; j++) {
                assertThat(topics[i].getTopicName())
                    .isNotEqualTo(topics[j].getTopicName());
            }
        }
    }
    
    @Test
    @DisplayName("각 이벤트 타입은 하나의 토픽에만 속한다")
    void testEventTypeUniqueness() {
        // Given
        KafkaTopic[] topics = KafkaTopic.values();
        
        // When & Then
        for (KafkaTopic topic1 : topics) {
            for (String eventType : topic1.getEventTypes()) {
                int count = 0;
                for (KafkaTopic topic2 : topics) {
                    if (topic2.containsEventType(eventType)) {
                        count++;
                    }
                }
                assertThat(count)
                    .as("Event type '%s' should belong to exactly one topic", eventType)
                    .isEqualTo(1);
            }
        }
    }
}