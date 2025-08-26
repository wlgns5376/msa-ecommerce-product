package com.commerce.product.domain.model;

import com.commerce.common.event.DomainEvent;
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
     * @deprecated pullDomainEvents() 메소드를 사용하세요. 더 명확한 이름으로 동일한 기능을 제공합니다.
     */
    @Deprecated
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
     * 도메인 이벤트를 반환하고 내부 목록을 비웁니다.
     * 이 메소드는 한 번의 호출로 이벤트 목록을 가져오고 비우는 작업을 모두 수행하여,
     * 이벤트 발행 후 clearDomainEvents()를 호출하는 것을 잊는 실수를 방지합니다.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulledEvents = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(pulledEvents);
    }
    
    /**
     * 애그리게이트의 ID를 반환합니다.
     */
    public abstract ID getId();
}