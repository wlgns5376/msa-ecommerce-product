package com.commerce.inventory.infrastructure.event.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis를 사용한 멱등성 서비스 구현
 */
@Service
public class RedisIdempotencyService implements IdempotencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisIdempotencyService.class);
    private static final String KEY_PREFIX = "event:processed:";
    private static final Duration TTL = Duration.ofDays(7);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RedisIdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public boolean isProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            logger.warn("Invalid eventId provided: {}", eventId);
            return false;
        }
        
        try {
            String key = KEY_PREFIX + eventId;
            String value = redisTemplate.opsForValue().get(key);
            boolean processed = value != null;
            
            if (processed) {
                logger.debug("Event {} has already been processed", eventId);
            }
            
            return processed;
        } catch (Exception e) {
            logger.error("Error checking if event {} is processed", eventId, e);
            return false;
        }
    }
    
    @Override
    public void markAsProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            logger.warn("Invalid eventId provided: {}", eventId);
            return;
        }
        
        try {
            String key = KEY_PREFIX + eventId;
            redisTemplate.opsForValue().set(key, "processed", TTL);
            logger.debug("Marked event {} as processed", eventId);
        } catch (Exception e) {
            logger.error("Error marking event {} as processed", eventId, e);
        }
    }
}