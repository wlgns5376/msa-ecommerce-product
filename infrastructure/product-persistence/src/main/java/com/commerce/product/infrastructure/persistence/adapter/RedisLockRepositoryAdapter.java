package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.product.domain.model.DistributedLock;
import com.commerce.product.domain.repository.LockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisLockRepositoryAdapter implements LockRepository {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String LOCK_PREFIX = "distributed_lock:";
    private static final String RELEASE_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('del', KEYS[1]) " +
        "else " +
        "   return 0 " +
        "end";
    
    private static final String EXTEND_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('pexpire', KEYS[1], ARGV[2]) " +
        "else " +
        "   return 0 " +
        "end";
    
    @Override
    public Optional<DistributedLock> acquireLock(String key, Duration leaseDuration, Duration waitTimeout) {
        String lockKey = LOCK_PREFIX + key;
        String lockId = UUID.randomUUID().toString();
        
        long startTime = System.currentTimeMillis();
        long waitTimeoutMillis = waitTimeout.toMillis();
        
        while (System.currentTimeMillis() - startTime < waitTimeoutMillis) {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockId, leaseDuration);
            
            if (Boolean.TRUE.equals(acquired)) {
                DistributedLock lock = new DistributedLock(key, lockId, Instant.now(), leaseDuration);
                log.debug("Lock acquired: key={}, lockId={}, leaseDuration={}ms", 
                    key, lockId, leaseDuration.toMillis());
                return Optional.of(lock);
            }
            
            try {
                Thread.sleep(50); // 50ms 대기 후 재시도
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Lock acquisition interrupted: key={}", key);
                return Optional.empty();
            }
        }
        
        log.warn("Failed to acquire lock within timeout: key={}, timeout={}ms", 
            key, waitTimeoutMillis);
        return Optional.empty();
    }
    
    @Override
    public boolean releaseLock(DistributedLock lock) {
        String lockKey = LOCK_PREFIX + lock.key();
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RELEASE_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(
            script, 
            Collections.singletonList(lockKey), 
            lock.lockId()
        );
        
        boolean released = result != null && result > 0;
        
        if (released) {
            log.debug("Lock released: key={}, lockId={}", lock.key(), lock.lockId());
        } else {
            log.warn("Failed to release lock (already released or expired): key={}, lockId={}", 
                lock.key(), lock.lockId());
        }
        
        return released;
    }
    
    @Override
    public boolean extendLock(DistributedLock lock, Duration additionalTime) {
        String lockKey = LOCK_PREFIX + lock.key();
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(EXTEND_SCRIPT);
        script.setResultType(Long.class);
        
        long totalDurationMillis = lock.leaseDuration().toMillis() + additionalTime.toMillis();
        
        Long result = redisTemplate.execute(
            script, 
            Collections.singletonList(lockKey), 
            lock.lockId(),
            String.valueOf(totalDurationMillis)
        );
        
        boolean extended = result != null && result > 0;
        
        if (extended) {
            log.debug("Lock extended: key={}, lockId={}, additionalTime={}ms", 
                lock.key(), lock.lockId(), additionalTime.toMillis());
        } else {
            log.warn("Failed to extend lock (not owned or expired): key={}, lockId={}", 
                lock.key(), lock.lockId());
        }
        
        return extended;
    }
    
    @Override
    public boolean isLocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
}