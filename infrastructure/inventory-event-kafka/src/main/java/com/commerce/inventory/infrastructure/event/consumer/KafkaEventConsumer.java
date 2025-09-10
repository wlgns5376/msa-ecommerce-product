package com.commerce.inventory.infrastructure.event.consumer;

import com.commerce.inventory.infrastructure.event.handler.EventHandler;
import com.commerce.inventory.infrastructure.event.handler.EventHandlerRegistry;
import com.commerce.inventory.infrastructure.event.idempotency.IdempotencyService;
import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 이벤트 컨슈머
 */
@Component
public class KafkaEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumer.class);
    
    private final EventHandlerRegistry handlerRegistry;
    private final IdempotencyService idempotencyService;
    
    public KafkaEventConsumer(EventHandlerRegistry handlerRegistry, 
                             IdempotencyService idempotencyService) {
        this.handlerRegistry = handlerRegistry;
        this.idempotencyService = idempotencyService;
    }
    
    @KafkaListener(
        topics = "${kafka.topics.inventory-events:inventory-events}",
        groupId = "${kafka.consumer.group-id:inventory-consumer-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, EventMessage> record, Acknowledgment acknowledgment) {
        EventMessage eventMessage = record.value();
        String eventId = eventMessage.getEventId();
        String eventType = eventMessage.getEventType();
        
        logger.info("Received event: eventId={}, eventType={}, aggregateId={}", 
                   eventId, eventType, eventMessage.getAggregateId());
        
        try {
            // 멱등성 체크
            if (idempotencyService.isProcessed(eventId)) {
                logger.info("Event {} has already been processed, skipping", eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            // 이벤트 핸들러 조회
            EventHandler handler = handlerRegistry.getHandler(eventType);
            if (handler == null) {
                logger.warn("No handler registered for event type: {}", eventType);
                acknowledgment.acknowledge();
                return;
            }
            
            // 이벤트 처리
            CompletableFuture<Void> future = handler.handle(eventMessage);
            future.whenComplete((result, error) -> {
                if (error != null) {
                    logger.error("Failed to process event: eventId={}, eventType={}", 
                               eventId, eventType, error);
                    // 에러 발생 시 acknowledgment하지 않아 재처리되도록 함
                } else {
                    logger.info("Successfully processed event: eventId={}, eventType={}", 
                              eventId, eventType);
                    idempotencyService.markAsProcessed(eventId);
                    acknowledgment.acknowledge();
                }
            }).join(); // 동기적으로 처리 완료를 기다림
            
        } catch (Exception e) {
            logger.error("Error processing event: eventId={}, eventType={}", 
                       eventId, eventType, e);
            // 예외 발생 시 acknowledgment하지 않아 재처리되도록 함
        }
    }
    
    @KafkaListener(
        topics = "${kafka.topics.product-events:product-events}",
        groupId = "${kafka.consumer.group-id:inventory-consumer-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProductEvents(
            @Payload EventMessage eventMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("Received product event from topic={}, partition={}, offset={}, eventType={}", 
                   topic, partition, offset, eventMessage.getEventType());
        
        try {
            // 멱등성 체크
            if (idempotencyService.isProcessed(eventMessage.getEventId())) {
                logger.info("Product event {} has already been processed, skipping", 
                          eventMessage.getEventId());
                acknowledgment.acknowledge();
                return;
            }
            
            // 이벤트 핸들러 조회 및 처리
            EventHandler handler = handlerRegistry.getHandler(eventMessage.getEventType());
            if (handler != null) {
                handler.handle(eventMessage)
                    .whenComplete((result, error) -> {
                        if (error == null) {
                            idempotencyService.markAsProcessed(eventMessage.getEventId());
                            acknowledgment.acknowledge();
                            logger.info("Successfully processed product event: {}", 
                                      eventMessage.getEventId());
                        } else {
                            logger.error("Failed to process product event: {}", 
                                       eventMessage.getEventId(), error);
                        }
                    }).join();
            } else {
                logger.warn("No handler for product event type: {}", eventMessage.getEventType());
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            logger.error("Error processing product event: {}", eventMessage.getEventId(), e);
        }
    }
}