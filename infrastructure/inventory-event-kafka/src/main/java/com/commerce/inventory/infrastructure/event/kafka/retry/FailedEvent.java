package com.commerce.inventory.infrastructure.event.kafka.retry;

import com.commerce.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실패한 이벤트 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedEvent {
    private String id;
    private DomainEvent event;
    private String failureReason;
    private LocalDateTime failedAt;
    private int retryCount;
    private LocalDateTime lastRetryAt;
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }
}