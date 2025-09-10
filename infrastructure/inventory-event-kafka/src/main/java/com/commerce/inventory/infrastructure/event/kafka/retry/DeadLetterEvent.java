package com.commerce.inventory.infrastructure.event.kafka.retry;

import com.commerce.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dead Letter Queue의 이벤트 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEvent {
    private String id;
    private DomainEvent event;
    private String reason;
    private LocalDateTime movedAt;
}