package com.commerce.common.domain.model;

import java.time.Clock;
import java.time.LocalDateTime;

public abstract class BaseEntity {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    protected BaseEntity() {
        this(Clock.systemDefaultZone());
    }
    
    protected BaseEntity(Clock clock) {
        this.createdAt = LocalDateTime.now(clock);
        this.updatedAt = LocalDateTime.now(clock);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    protected void updateTimestamp() {
        updateTimestamp(Clock.systemDefaultZone());
    }
    
    protected void updateTimestamp(Clock clock) {
        this.updatedAt = LocalDateTime.now(clock);
    }
}