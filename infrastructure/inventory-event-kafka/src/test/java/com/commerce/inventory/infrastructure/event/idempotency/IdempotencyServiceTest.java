package com.commerce.inventory.infrastructure.event.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new RedisIdempotencyService(redisTemplate);
    }

    @Test
    void shouldReturnFalseWhenEventNotProcessed() {
        // Given
        String eventId = UUID.randomUUID().toString();
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        boolean processed = idempotencyService.isProcessed(eventId);

        // Then
        assertThat(processed).isFalse();
        verify(valueOperations).get("event:processed:" + eventId);
    }

    @Test
    void shouldReturnTrueWhenEventAlreadyProcessed() {
        // Given
        String eventId = UUID.randomUUID().toString();
        when(valueOperations.get(anyString())).thenReturn("processed");

        // When
        boolean processed = idempotencyService.isProcessed(eventId);

        // Then
        assertThat(processed).isTrue();
        verify(valueOperations).get("event:processed:" + eventId);
    }

    @Test
    void shouldMarkEventAsProcessed() {
        // Given
        String eventId = UUID.randomUUID().toString();

        // When
        idempotencyService.markAsProcessed(eventId);

        // Then
        verify(valueOperations).set(
                eq("event:processed:" + eventId),
                eq("processed"),
                eq(Duration.ofDays(7))
        );
    }

    @Test
    void shouldHandleNullEventId() {
        // When
        boolean processed = idempotencyService.isProcessed(null);

        // Then
        assertThat(processed).isFalse();
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void shouldHandleEmptyEventId() {
        // When
        boolean processed = idempotencyService.isProcessed("");

        // Then
        assertThat(processed).isFalse();
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void shouldNotMarkNullEventIdAsProcessed() {
        // When
        idempotencyService.markAsProcessed(null);

        // Then
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void shouldNotMarkEmptyEventIdAsProcessed() {
        // When
        idempotencyService.markAsProcessed("");

        // Then
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void shouldHandleRedisException() {
        // Given
        String eventId = UUID.randomUUID().toString();
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean processed = idempotencyService.isProcessed(eventId);

        // Then
        assertThat(processed).isFalse();
    }
}