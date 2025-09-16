package com.commerce.inventory.infrastructure.event.kafka;

import com.commerce.common.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KafkaTopicResolver 테스트")
class KafkaTopicResolverTest {
    
    private KafkaTopicResolver topicResolver;
    
    @BeforeEach
    void setUp() {
        topicResolver = new KafkaTopicResolver();
        ReflectionTestUtils.setField(topicResolver, "topicPrefix", "inventory");
        ReflectionTestUtils.setField(topicResolver, "defaultTopic", "inventory-events");
    }
    
    @ParameterizedTest
    @DisplayName("Enum에 정의된 Stock 이벤트 타입에 대해 올바른 토픽을 반환한다")
    @CsvSource({
        "StockReceivedEvent, inventory-stock-events",
        "StockReservedEvent, inventory-stock-events",
        "StockDepletedEvent, inventory-stock-events",
        "ReservationReleasedEvent, inventory-stock-events"
    })
    void testResolveStockEventTopics(String eventType, String expectedTopic) {
        // Given
        TestEvent event = new TestEvent(eventType);
        
        // When
        String topic = topicResolver.resolveTopic(event);
        
        // Then
        assertThat(topic).isEqualTo(expectedTopic);
    }
    
    @ParameterizedTest
    @DisplayName("Enum에 정의된 SKU 이벤트 타입에 대해 올바른 토픽을 반환한다")
    @CsvSource({
        "SkuCreatedEvent, inventory-sku-events",
        "SkuUpdatedEvent, inventory-sku-events",
        "SkuDeletedEvent, inventory-sku-events"
    })
    void testResolveSkuEventTopics(String eventType, String expectedTopic) {
        // Given
        TestEvent event = new TestEvent(eventType);
        
        // When
        String topic = topicResolver.resolveTopic(event);
        
        // Then
        assertThat(topic).isEqualTo(expectedTopic);
    }
    
    @Test
    @DisplayName("Enum에 정의된 Movement 이벤트 타입에 대해 올바른 토픽을 반환한다")
    void testResolveMovementEventTopic() {
        // Given
        TestEvent event = new TestEvent("StockMovementCreatedEvent");
        
        // When
        String topic = topicResolver.resolveTopic(event);
        
        // Then
        assertThat(topic).isEqualTo("inventory-movement-events");
    }
    
    @Test
    @DisplayName("커스텀 매핑을 추가하면 우선적으로 적용된다")
    void testCustomMapping() {
        // Given
        String eventType = "CustomEvent";
        String customTopic = "custom-topic";
        TestEvent event = new TestEvent(eventType);
        
        // When
        topicResolver.addCustomMapping(eventType, customTopic);
        String topic = topicResolver.resolveTopic(event);
        
        // Then
        assertThat(topic).isEqualTo(customTopic);
    }
    
    @Test
    @DisplayName("커스텀 매핑이 Enum 매핑보다 우선순위가 높다")
    void testCustomMappingOverridesEnum() {
        // Given
        String eventType = "StockReceivedEvent";
        String customTopic = "custom-stock-topic";
        TestEvent event = new TestEvent(eventType);
        
        // When
        topicResolver.addCustomMapping(eventType, customTopic);
        String topic = topicResolver.resolveTopic(event);
        
        // Then
        assertThat(topic).isEqualTo(customTopic);
    }
    
    @Test
    @DisplayName("커스텀 매핑을 제거하면 Enum 매핑이 적용된다")
    void testRemoveCustomMapping() {
        // Given
        String eventType = "StockReceivedEvent";
        String customTopic = "custom-stock-topic";
        TestEvent event = new TestEvent(eventType);
        
        // When
        topicResolver.addCustomMapping(eventType, customTopic);
        topicResolver.removeCustomMapping(eventType);
        String topic = topicResolver.resolveTopic(event);
        
        // Then
        assertThat(topic).isEqualTo("inventory-stock-events");
    }
    
    @ParameterizedTest
    @DisplayName("Enum에 정의되지 않은 이벤트는 동적으로 토픽 이름을 생성한다")
    @CsvSource({
        "UnknownEvent, inventory-unknown",
        "OrderCreatedEvent, inventory-order-created",
        "PaymentProcessedEvent, inventory-payment-processed"
    })
    void testGenerateTopicForUnknownEvents(String eventType, String expectedTopic) {
        // Given
        TestEvent event = new TestEvent(eventType);
        
        // When
        String topic = topicResolver.resolveTopic(event);
        
        // Then
        assertThat(topic).isEqualTo(expectedTopic);
    }
    
    // Test helper class
    static class TestEvent implements DomainEvent {
        private final String eventType;
        private final LocalDateTime occurredAt = LocalDateTime.now();
        
        TestEvent(String eventType) {
            this.eventType = eventType;
        }
        
        @Override
        public String eventType() {
            return eventType;
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
    }
}