package com.commerce.common.domain.model;

import com.commerce.common.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot<ID> extends BaseEntity {
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected AggregateRoot() {
        super();
    }
    
    protected AggregateRoot(LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
    }
    
    /**
     * 도메인 이벤트를 추가합니다.
     */
    protected void raise(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    /**
     * 도메인 이벤트를 추가합니다.
     */
    protected void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    /**
     * 도메인 이벤트 목록을 반환합니다. (읽기 전용)
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }
    
    /**
     * 도메인 이벤트를 반환하고 내부 목록을 비웁니다.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulledEvents = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(pulledEvents);
    }
    
    /**
     * 모든 도메인 이벤트를 반환하고 클리어합니다.
     * @deprecated pullDomainEvents() 메소드를 사용하세요.
     */
    @Deprecated
    public List<DomainEvent> clearDomainEvents() {
        return pullDomainEvents();
    }
    
    public abstract ID getId();
}