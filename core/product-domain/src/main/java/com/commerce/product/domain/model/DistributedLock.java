package com.commerce.product.domain.model;

import java.time.Duration;
import java.time.Instant;

public record DistributedLock(
    String key,
    String lockId,
    Instant acquiredAt,
    Duration leaseDuration
) {
    
    public boolean isExpired() {
        return Instant.now().isAfter(acquiredAt.plus(leaseDuration));
    }
    
    public long getRemainingTimeMillis() {
        Instant expiryTime = acquiredAt.plus(leaseDuration);
        Duration remaining = Duration.between(Instant.now(), expiryTime);
        return remaining.isNegative() ? 0 : remaining.toMillis();
    }
}