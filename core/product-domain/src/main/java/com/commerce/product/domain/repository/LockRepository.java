package com.commerce.product.domain.repository;

import java.util.concurrent.locks.Lock;

public interface LockRepository extends Repository {
    
    Lock acquireLock(String key, long timeoutMillis);
    
    void releaseLock(Lock lock);
}