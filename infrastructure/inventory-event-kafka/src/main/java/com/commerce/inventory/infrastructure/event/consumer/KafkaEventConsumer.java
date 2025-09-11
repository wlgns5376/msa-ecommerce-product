package com.commerce.inventory.infrastructure.event.consumer;

import com.commerce.inventory.infrastructure.event.dlq.DeadLetterQueueService;
import com.commerce.inventory.infrastructure.event.handler.EventHandler;
import com.commerce.inventory.infrastructure.event.handler.EventHandlerRegistry;
import com.commerce.inventory.infrastructure.event.idempotency.IdempotencyService;
import com.commerce.inventory.infrastructure.event.retry.RetryService;
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
import java.util.concurrent.TimeUnit;

/**
 * Kafka 이벤트 컨슈머
 * 재시도 메커니즘과 Dead Letter Queue를 통해 안정적인 메시지 처리를 보장
 */
@Component
public class KafkaEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumer.class);
    
    private final EventHandlerRegistry handlerRegistry;
    private final IdempotencyService idempotencyService;
    private final RetryService retryService;
    private final DeadLetterQueueService deadLetterQueueService;
    
    public KafkaEventConsumer(EventHandlerRegistry handlerRegistry, 
                             IdempotencyService idempotencyService,
                             RetryService retryService,
                             DeadLetterQueueService deadLetterQueueService) {
        this.handlerRegistry = handlerRegistry;
        this.idempotencyService = idempotencyService;
        this.retryService = retryService;
        this.deadLetterQueueService = deadLetterQueueService;
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
                    handleProcessingError(eventMessage, "inventory-events", error, acknowledgment);
                } else {
                    logger.info("Successfully processed event: eventId={}, eventType={}", 
                              eventId, eventType);
                    idempotencyService.markAsProcessed(eventId);
                    retryService.clearRetryInfo(eventId);
                    acknowledgment.acknowledge();
                }
            }).join(); // 동기적으로 처리 완료를 기다림
            
        } catch (Exception e) {
            handleProcessingError(eventMessage, "inventory-events", e, acknowledgment);
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
                            retryService.clearRetryInfo(eventMessage.getEventId());
                            acknowledgment.acknowledge();
                            logger.info("Successfully processed product event: {}", 
                                      eventMessage.getEventId());
                        } else {
                            handleProcessingError(eventMessage, topic, error, acknowledgment);
                        }
                    }).join();
            } else {
                logger.warn("No handler for product event type: {}", eventMessage.getEventType());
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            handleProcessingError(eventMessage, topic, e, acknowledgment);
        }
    }
    
    /**
     * 처리 에러 핸들링
     * 재시도 로직과 DLQ 전송을 처리
     */
    private void handleProcessingError(EventMessage eventMessage, String topic, 
                                      Throwable error, Acknowledgment acknowledgment) {
        String eventId = eventMessage.getEventId();
        String eventType = eventMessage.getEventType();
        
        logger.error("Failed to process event: eventId={}, eventType={}, error={}", 
                    eventId, eventType, error.getMessage(), error);
        
        // 재시도 가능 여부 확인
        if (retryService.shouldRetry(eventId)) {
            int retryCount = retryService.incrementRetryCount(eventId);
            long backoffMillis = retryService.calculateBackoffMillis(eventId);
            
            logger.info("Scheduling retry {} for event {} after {}ms", 
                       retryCount, eventId, backoffMillis);
            
            try {
                // 백오프 시간만큼 대기
                Thread.sleep(backoffMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.warn("Retry backoff interrupted for event: {}", eventId);
            }
            
            // acknowledgment하지 않아 메시지가 다시 처리되도록 함
            logger.info("Event {} will be retried (attempt {} of max attempts)", 
                       eventId, retryCount);
            
        } else {
            // 최대 재시도 횟수 초과 - DLQ로 전송
            int finalRetryCount = retryService.getRetryCount(eventId);
            logger.error("Max retries ({}) reached for event: {}. Sending to DLQ.", 
                        finalRetryCount, eventId);
            
            deadLetterQueueService.sendToDeadLetterQueue(eventMessage, topic, error, finalRetryCount);
            
            // DLQ로 전송 후 메시지를 acknowledge하여 offset을 이동
            acknowledgment.acknowledge();
            
            // 재시도 정보 정리
            retryService.clearRetryInfo(eventId);
        }
    }
}