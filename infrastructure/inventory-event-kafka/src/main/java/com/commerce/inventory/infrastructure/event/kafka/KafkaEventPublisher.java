package com.commerce.inventory.infrastructure.event.kafka;

import com.commerce.common.event.DomainEvent;
import com.commerce.inventory.application.service.port.out.EventPublisher;
import com.commerce.inventory.infrastructure.event.kafka.retry.RetryableEventStore;
import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import com.commerce.inventory.infrastructure.event.serialization.EventSerializer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka를 사용한 EventPublisher 구현체
 * 도메인 이벤트를 Kafka 토픽으로 발행합니다.
 */
@Slf4j
@Component("kafkaEventPublisher")
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {
    
    private final KafkaTemplate<String, EventMessage> kafkaTemplate;
    private final EventSerializer eventSerializer;
    private final KafkaTopicResolver topicResolver;
    private final KafkaErrorHandler errorHandler;
    private final RetryableEventStore retryableEventStore;
    
    @PostConstruct
    public void init() {
        // RetryableEventStore에 publish 함수 설정
        retryableEventStore.setRetryPublisher(this::publish);
    }
    
    @Override
    public void publish(DomainEvent event) {
        try {
            String topic = topicResolver.resolveTopic(event);
            EventMessage message = eventSerializer.serialize(event);
            String key = generateKey(event);
            
            log.debug("Publishing event to topic: {}, key: {}, eventType: {}", 
                topic, key, event.eventType());
            
            CompletableFuture<SendResult<String, EventMessage>> future = 
                kafkaTemplate.send(topic, key, message);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    handlePublishError(event, ex);
                } else {
                    handlePublishSuccess(event, result);
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event, e);
            errorHandler.handleError(event, e);
        }
    }
    
    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
    
    private String generateKey(DomainEvent event) {
        // Extract aggregate ID from event for partitioning
        if (event instanceof AggregateEvent) {
            return ((AggregateEvent) event).getAggregateId();
        }
        // Default to event type for non-aggregate events
        return event.eventType();
    }
    
    private void handlePublishSuccess(DomainEvent event, SendResult<String, EventMessage> result) {
        log.info("Successfully published event: {} to partition: {} at offset: {}",
            event.eventType(),
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
    }
    
    private void handlePublishError(DomainEvent event, Throwable error) {
        log.error("Failed to publish event: {}", event.eventType(), error);
        errorHandler.handleError(event, error);
    }
}