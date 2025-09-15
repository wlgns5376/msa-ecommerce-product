package com.commerce.inventory.infrastructure.event.kafka.retry;

import com.commerce.common.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 재시도 가능한 이벤트 저장소
 * 실패한 이벤트를 임시 저장하고 재시도를 관리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryableEventStore {
    
    private final ObjectMapper objectMapper;
    
    @Setter
    private Consumer<DomainEvent> retryPublisher;
    
    // In-memory store for failed events (실제 환경에서는 Redis나 DB 사용 권장)
    private final Map<String, FailedEvent> failedEvents = new ConcurrentHashMap<>();
    private final Map<String, DeadLetterEvent> deadLetterQueue = new ConcurrentHashMap<>();
    
    /**
     * 이벤트 재시도
     */
    public void retry(DomainEvent event) {
        log.info("Retrying event: {}", event.eventType());
        if (retryPublisher != null) {
            retryPublisher.accept(event);
        } else {
            log.error("Retry publisher not set for event: {}", event.eventType());
        }
    }
    
    /**
     * 실패한 이벤트 저장
     */
    public void storeFailedEvent(DomainEvent event, String reason) {
        String id = UUID.randomUUID().toString();
        FailedEvent failedEvent = FailedEvent.builder()
            .id(id)
            .event(event)
            .failureReason(reason)
            .failedAt(LocalDateTime.now())
            .retryCount(0)
            .build();
        
        failedEvents.put(id, failedEvent);
        log.info("Stored failed event: {} with id: {}", event.eventType(), id);
    }
    
    /**
     * Dead Letter Queue로 이동
     */
    public void moveToDeadLetter(DomainEvent event, String reason) {
        String id = UUID.randomUUID().toString();
        DeadLetterEvent deadLetterEvent = DeadLetterEvent.builder()
            .id(id)
            .event(event)
            .reason(reason)
            .movedAt(LocalDateTime.now())
            .build();
        
        deadLetterQueue.put(id, deadLetterEvent);
        log.warn("Moved event to DLQ: {} with id: {}", event.eventType(), id);
    }
    
    /**
     * 실패한 이벤트 조회
     */
    public Map<String, FailedEvent> getFailedEvents() {
        return new ConcurrentHashMap<>(failedEvents);
    }
    
    /**
     * Dead Letter Queue 조회
     */
    public Map<String, DeadLetterEvent> getDeadLetterQueue() {
        return new ConcurrentHashMap<>(deadLetterQueue);
    }
    
    /**
     * 실패한 이벤트 제거
     */
    public void removeFailedEvent(String id) {
        failedEvents.remove(id);
    }
    
    /**
     * Dead Letter Queue에서 제거
     */
    public void removeFromDeadLetter(String id) {
        deadLetterQueue.remove(id);
    }
    
    /**
     * 모든 실패한 이벤트 재시도
     */
    public void retryAllFailedEvents() {
        failedEvents.values().forEach(failedEvent -> {
            try {
                retry(failedEvent.getEvent());
                removeFailedEvent(failedEvent.getId());
            } catch (Exception e) {
                log.error("Failed to retry event: {}", failedEvent.getId(), e);
                failedEvent.incrementRetryCount();
            }
        });
    }
    
    /**
     * Dead Letter Queue의 이벤트를 재시도 큐로 이동
     */
    public void reprocessDeadLetterEvent(String id) {
        DeadLetterEvent deadLetterEvent = deadLetterQueue.get(id);
        if (deadLetterEvent != null) {
            storeFailedEvent(deadLetterEvent.getEvent(), "Reprocessing from DLQ");
            removeFromDeadLetter(id);
            log.info("Moved event from DLQ to retry queue: {}", id);
        }
    }
}