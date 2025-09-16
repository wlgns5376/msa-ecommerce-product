package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import java.time.LocalDateTime;

public record FailedEventRecord(
    String eventId,
    DomainEvent event,
    String eventType,
    LocalDateTime failedAt,
    int retryCount,
    String errorMessage,
    String stackTrace
) {}