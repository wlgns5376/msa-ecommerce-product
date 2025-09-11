package com.commerce.inventory.infrastructure.event.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메모리 기반 멱등성 서비스 구현 (개발/테스트용)
 */
@Service
@ConditionalOnMissingBean(RedisIdempotencyService.class)
public class InMemoryIdempotencyService implements IdempotencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryIdempotencyService.class);
    private static final long TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L; // 7일
    
    private final Map<String, Instant> processedEvents = new ConcurrentHashMap<>();
    
    @Override
    public boolean isProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            logger.warn("Invalid eventId provided: {}", eventId);
            return false;
        }
        
        cleanExpiredEvents();
        
        boolean processed = processedEvents.containsKey(eventId);
        if (processed) {
            logger.debug("Event {} has already been processed", eventId);
        }
        
        return processed;
    }
    
    @Override
    public void markAsProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            logger.warn("Invalid eventId provided: {}", eventId);
            return;
        }
        
        processedEvents.put(eventId, Instant.now());
        logger.debug("Marked event {} as processed", eventId);
    }
    
    /**
     * 만료된 이벤트를 정리합니다.
     */
    private void cleanExpiredEvents() {
        Instant now = Instant.now();
        processedEvents.entrySet().removeIf(entry -> {
            Instant processedTime = entry.getValue();
            return now.toEpochMilli() - processedTime.toEpochMilli() > TTL_MILLIS;
        });
    }
}