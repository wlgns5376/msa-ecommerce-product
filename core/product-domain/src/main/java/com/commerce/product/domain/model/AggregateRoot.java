package com.commerce.product.domain.model;

import com.commerce.product.common.event.DomainEvent;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 애그리게이트 루트의 기본 클래스
 * 도메인 이벤트 발행 기능을 제공합니다.
 */
@Getter
public abstract class AggregateRoot<ID> {
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    /**
     * 도메인 이벤트를 추가합니다.
     */
    protected void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    /**
     * 모든 도메인 이벤트를 반환하고 클리어합니다.
     */
    public List<DomainEvent> clearDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }
    
    /**
     * 도메인 이벤트 목록을 반환합니다. (읽기 전용)
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(this.domainEvents);
    }
    
    /**
     * 애그리게이트의 ID를 반환합니다.
     */
    public abstract ID getId();
}