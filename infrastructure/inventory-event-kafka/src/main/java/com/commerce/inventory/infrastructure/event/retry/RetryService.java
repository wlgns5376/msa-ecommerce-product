package com.commerce.inventory.infrastructure.event.retry;

import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 이벤트 재시도 서비스
 */
@Service
public class RetryService {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryService.class);
    
    private final RetryConfiguration retryConfiguration;
    private final ConcurrentHashMap<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastRetryTimeMap = new ConcurrentHashMap<>();
    
    public RetryService(RetryConfiguration retryConfiguration) {
        this.retryConfiguration = retryConfiguration;
    }
    
    /**
     * 재시도 가능 여부 확인
     */
    public boolean shouldRetry(String eventId) {
        AtomicInteger retryCount = retryCountMap.computeIfAbsent(eventId, k -> new AtomicInteger(0));
        return retryCount.get() < retryConfiguration.getMaxAttempts();
    }
    
    /**
     * 재시도 카운트 증가
     */
    public int incrementRetryCount(String eventId) {
        AtomicInteger retryCount = retryCountMap.computeIfAbsent(eventId, k -> new AtomicInteger(0));
        int attempts = retryCount.incrementAndGet();
        lastRetryTimeMap.put(eventId, System.currentTimeMillis());
        
        logger.info("Retry attempt {} of {} for event: {}", 
                   attempts, retryConfiguration.getMaxAttempts(), eventId);
        
        return attempts;
    }
    
    /**
     * 재시도 백오프 시간 계산
     */
    public long calculateBackoffMillis(String eventId) {
        AtomicInteger retryCount = retryCountMap.get(eventId);
        if (retryCount == null) {
            return retryConfiguration.getBackoffMillis();
        }
        
        int attempts = retryCount.get();
        long backoff = (long) (retryConfiguration.getBackoffMillis() * 
                               Math.pow(retryConfiguration.getBackoffMultiplier(), attempts - 1));
        
        return Math.min(backoff, retryConfiguration.getMaxBackoffMillis());
    }
    
    /**
     * 재시도 정보 초기화
     */
    public void clearRetryInfo(String eventId) {
        retryCountMap.remove(eventId);
        lastRetryTimeMap.remove(eventId);
        logger.debug("Cleared retry info for event: {}", eventId);
    }
    
    /**
     * 재시도 횟수 조회
     */
    public int getRetryCount(String eventId) {
        AtomicInteger count = retryCountMap.get(eventId);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 최대 재시도 횟수 도달 여부
     */
    public boolean isMaxRetriesReached(String eventId) {
        return getRetryCount(eventId) >= retryConfiguration.getMaxAttempts();
    }
}