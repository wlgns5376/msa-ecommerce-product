package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.product.domain.model.DistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisLockRepositoryAdapterTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    private RedisLockRepositoryAdapter lockRepository;
    
    @BeforeEach
    void setUp() {
        lockRepository = new RedisLockRepositoryAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    @DisplayName("락 획득에 성공하면 DistributedLock을 반환한다")
    void acquireLock_whenSuccessful_returnsLock() {
        // Given
        String key = "test-lock";
        Duration leaseDuration = Duration.ofSeconds(30);
        Duration waitTimeout = Duration.ofSeconds(5);
        
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);
        
        // When
        Optional<DistributedLock> result = lockRepository.acquireLock(key, leaseDuration, waitTimeout);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().key()).isEqualTo(key);
        assertThat(result.get().leaseDuration()).isEqualTo(leaseDuration);
        assertThat(result.get().lockId()).isNotEmpty();
        
        verify(valueOperations).setIfAbsent(
            eq("distributed_lock:" + key), 
            anyString(), 
            eq(leaseDuration)
        );
    }
    
    @Test
    @DisplayName("락 획득에 실패하면 Empty Optional을 반환한다")
    void acquireLock_whenTimeout_returnsEmpty() {
        // Given
        String key = "test-lock";
        Duration leaseDuration = Duration.ofSeconds(30);
        Duration waitTimeout = Duration.ofMillis(100);
        
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false);
        
        // When
        Optional<DistributedLock> result = lockRepository.acquireLock(key, leaseDuration, waitTimeout);
        
        // Then
        assertThat(result).isEmpty();
        
        verify(valueOperations, atLeastOnce()).setIfAbsent(
            eq("distributed_lock:" + key), 
            anyString(), 
            eq(leaseDuration)
        );
    }
    
    @Test
    @DisplayName("락 해제에 성공하면 true를 반환한다")
    void releaseLock_whenSuccessful_returnsTrue() {
        // Given
        String key = "test-lock";
        String lockId = UUID.randomUUID().toString();
        DistributedLock lock = new DistributedLock(key, lockId, Instant.now(), Duration.ofSeconds(30));
        
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
            .thenReturn(1L);
        
        // When
        boolean result = lockRepository.releaseLock(lock);
        
        // Then
        assertThat(result).isTrue();
        
        verify(redisTemplate).execute(
            any(DefaultRedisScript.class),
            eq(Collections.singletonList("distributed_lock:" + key)),
            eq(lockId)
        );
    }
    
    @Test
    @DisplayName("락 해제에 실패하면 false를 반환한다")
    void releaseLock_whenFailed_returnsFalse() {
        // Given
        String key = "test-lock";
        String lockId = UUID.randomUUID().toString();
        DistributedLock lock = new DistributedLock(key, lockId, Instant.now(), Duration.ofSeconds(30));
        
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
            .thenReturn(0L);
        
        // When
        boolean result = lockRepository.releaseLock(lock);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("락 연장에 성공하면 true를 반환한다")
    void extendLock_whenSuccessful_returnsTrue() {
        // Given
        String key = "test-lock";
        String lockId = UUID.randomUUID().toString();
        DistributedLock lock = new DistributedLock(key, lockId, Instant.now(), Duration.ofSeconds(30));
        Duration additionalTime = Duration.ofSeconds(20);
        
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class)))
            .thenReturn(1L);
        
        // When
        Optional<DistributedLock> result = lockRepository.extendLock(lock, additionalTime);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().key()).isEqualTo(key);
        assertThat(result.get().lockId()).isEqualTo(lockId);
        assertThat(result.get().leaseDuration()).isEqualTo(additionalTime);
        
        verify(redisTemplate).execute(
            any(DefaultRedisScript.class),
            eq(Collections.singletonList("distributed_lock:" + key)),
            eq(lockId),
            eq("20000")
        );
    }
    
    @Test
    @DisplayName("키가 잠겨있으면 true를 반환한다")
    void isLocked_whenKeyExists_returnsTrue() {
        // Given
        String key = "test-lock";
        when(redisTemplate.hasKey("distributed_lock:" + key)).thenReturn(true);
        
        // When
        boolean result = lockRepository.isLocked(key);
        
        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey("distributed_lock:" + key);
    }
    
    @Test
    @DisplayName("키가 잠겨있지 않으면 false를 반환한다")
    void isLocked_whenKeyNotExists_returnsFalse() {
        // Given
        String key = "test-lock";
        when(redisTemplate.hasKey("distributed_lock:" + key)).thenReturn(false);
        
        // When
        boolean result = lockRepository.isLocked(key);
        
        // Then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey("distributed_lock:" + key);
    }
}