package com.commerce.product.domain.repository;

import com.commerce.product.domain.model.DistributedLock;

import java.time.Duration;
import java.util.Optional;

public interface LockRepository {
    
    Optional<DistributedLock> acquireLock(String key, Duration leaseDuration, Duration waitTimeout);
    
    boolean releaseLock(DistributedLock lock);
    
    boolean extendLock(DistributedLock lock, Duration additionalTime);
    
    boolean isLocked(String key);
}