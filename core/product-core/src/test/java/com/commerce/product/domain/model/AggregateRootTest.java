package com.commerce.product.domain.model;

import com.commerce.common.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AggregateRoot 테스트")
class AggregateRootTest {
    
    private TestAggregate aggregate;
    
    @BeforeEach
    void setUp() {
        aggregate = new TestAggregate("test-id");
    }
    
    @Test
    @DisplayName("pullDomainEvents 호출 시 이벤트를 반환하고 내부 목록을 비운다")
    void pullDomainEvents_ReturnsAndClearsEvents() {
        // Given
        TestEvent event1 = new TestEvent("event1");
        TestEvent event2 = new TestEvent("event2");
        aggregate.addTestEvent(event1);
        aggregate.addTestEvent(event2);
        
        // When
        List<DomainEvent> pulledEvents = aggregate.pullDomainEvents();
        
        // Then
        assertEquals(2, pulledEvents.size());
        assertTrue(pulledEvents.contains(event1));
        assertTrue(pulledEvents.contains(event2));
        
        // 내부 이벤트 목록이 비워졌는지 확인
        assertTrue(aggregate.getDomainEvents().isEmpty());
    }
    
    @Test
    @DisplayName("pullDomainEvents 호출 시 불변 리스트를 반환한다")
    void pullDomainEvents_ReturnsUnmodifiableList() {
        // Given
        aggregate.addTestEvent(new TestEvent("event"));
        
        // When
        List<DomainEvent> pulledEvents = aggregate.pullDomainEvents();
        
        // Then
        assertThrows(UnsupportedOperationException.class, 
            () -> pulledEvents.add(new TestEvent("new event")));
    }
    
    @Test
    @DisplayName("이벤트가 없는 상태에서 pullDomainEvents 호출 시 빈 리스트를 반환한다")
    void pullDomainEvents_ReturnsEmptyListWhenNoEvents() {
        // When
        List<DomainEvent> pulledEvents = aggregate.pullDomainEvents();
        
        // Then
        assertTrue(pulledEvents.isEmpty());
    }
    
    @Test
    @DisplayName("clearDomainEvents와 pullDomainEvents는 동일하게 동작한다")
    void clearDomainEvents_And_pullDomainEvents_BehaveSimilarly() {
        // Given
        TestEvent event1 = new TestEvent("event1");
        TestEvent event2 = new TestEvent("event2");
        
        // Test clearDomainEvents
        aggregate.addTestEvent(event1);
        List<DomainEvent> clearedEvents = aggregate.clearDomainEvents();
        assertEquals(1, clearedEvents.size());
        assertTrue(aggregate.getDomainEvents().isEmpty());
        
        // Test pullDomainEvents
        aggregate.addTestEvent(event2);
        List<DomainEvent> pulledEvents = aggregate.pullDomainEvents();
        assertEquals(1, pulledEvents.size());
        assertTrue(aggregate.getDomainEvents().isEmpty());
    }
    
    static class TestAggregate extends AggregateRoot<String> {
        private final String id;
        
        public TestAggregate(String id) {
            this.id = id;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        public void addTestEvent(DomainEvent event) {
            addDomainEvent(event);
        }
    }
    
    static class TestEvent implements DomainEvent {
        private final String name;
        private final LocalDateTime occurredAt;
        
        public TestEvent(String name) {
            this.name = name;
            this.occurredAt = LocalDateTime.now();
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
        
        @Override
        public String eventType() {
            return "TestEvent";
        }
    }
}