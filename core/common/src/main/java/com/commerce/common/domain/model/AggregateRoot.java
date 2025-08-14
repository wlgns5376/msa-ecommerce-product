package com.commerce.common.domain.model;

import java.time.LocalDateTime;

public abstract class AggregateRoot<ID> extends BaseEntity {
    
    protected AggregateRoot() {
        super();
    }
    
    protected AggregateRoot(LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
    }
    
    public abstract ID getId();
}