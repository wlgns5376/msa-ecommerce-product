package com.commerce.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DistributedLockTest {
    
    @Test
    @DisplayName("락이 만료되지 않은 경우 false를 반환한다")
    void isExpired_whenNotExpired_returnsFalse() {
        // Given
        String key = "test-lock";
        String lockId = "lock-123";
        Instant acquiredAt = Instant.now();
        Duration leaseDuration = Duration.ofSeconds(30);
        
        DistributedLock lock = new DistributedLock(key, lockId, acquiredAt, leaseDuration);
        
        // When
        boolean isExpired = lock.isExpired();
        
        // Then
        assertThat(isExpired).isFalse();
    }
    
    @Test
    @DisplayName("락이 만료된 경우 true를 반환한다")
    void isExpired_whenExpired_returnsTrue() {
        // Given
        String key = "test-lock";
        String lockId = "lock-123";
        Instant acquiredAt = Instant.now().minus(Duration.ofMinutes(5));
        Duration leaseDuration = Duration.ofSeconds(30);
        
        DistributedLock lock = new DistributedLock(key, lockId, acquiredAt, leaseDuration);
        
        // When
        boolean isExpired = lock.isExpired();
        
        // Then
        assertThat(isExpired).isTrue();
    }
    
    @Test
    @DisplayName("남은 시간을 밀리초 단위로 반환한다")
    void getRemainingTimeMillis_returnsCorrectValue() {
        // Given
        String key = "test-lock";
        String lockId = "lock-123";
        Instant acquiredAt = Instant.now();
        Duration leaseDuration = Duration.ofSeconds(30);
        
        DistributedLock lock = new DistributedLock(key, lockId, acquiredAt, leaseDuration);
        
        // When
        long remainingTime = lock.getRemainingTimeMillis();
        
        // Then
        assertThat(remainingTime).isCloseTo(30000L, org.assertj.core.api.Assertions.within(1000L));
    }
    
    @Test
    @DisplayName("만료된 락의 남은 시간은 0이다")
    void getRemainingTimeMillis_whenExpired_returnsZero() {
        // Given
        String key = "test-lock";
        String lockId = "lock-123";
        Instant acquiredAt = Instant.now().minus(Duration.ofMinutes(5));
        Duration leaseDuration = Duration.ofSeconds(30);
        
        DistributedLock lock = new DistributedLock(key, lockId, acquiredAt, leaseDuration);
        
        // When
        long remainingTime = lock.getRemainingTimeMillis();
        
        // Then
        assertThat(remainingTime).isEqualTo(0);
    }
}