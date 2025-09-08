package com.commerce.product.api.adapter.out;

import com.commerce.common.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisherAdapter 테스트")
class EventPublisherAdapterTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private EventPublisherAdapter eventPublisherAdapter;

    private TestDomainEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent("test-aggregate-id", "test-data");
    }

    @Test
    @DisplayName("단일 도메인 이벤트를 성공적으로 발행한다")
    void testPublishSingleEvent() {
        // Given
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

        // When
        eventPublisherAdapter.publish(testEvent);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        DomainEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isEqualTo(testEvent);
        TestDomainEvent testCapturedEvent = (TestDomainEvent) capturedEvent;
        assertThat(testCapturedEvent.getAggregateId()).isEqualTo("test-aggregate-id");
    }

    @Test
    @DisplayName("여러 도메인 이벤트를 순차적으로 발행한다")
    void testPublishMultipleEvents() {
        // Given
        TestDomainEvent event1 = new TestDomainEvent("aggregate-1", "data-1");
        TestDomainEvent event2 = new TestDomainEvent("aggregate-2", "data-2");
        TestDomainEvent event3 = new TestDomainEvent("aggregate-3", "data-3");
        List<DomainEvent> events = Arrays.asList(event1, event2, event3);

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

        // When
        eventPublisherAdapter.publishAll(events);

        // Then
        verify(applicationEventPublisher, times(3)).publishEvent(eventCaptor.capture());
        List<DomainEvent> capturedEvents = eventCaptor.getAllValues();
        
        assertThat(capturedEvents).hasSize(3);
        assertThat(capturedEvents.get(0)).isEqualTo(event1);
        assertThat(capturedEvents.get(1)).isEqualTo(event2);
        assertThat(capturedEvents.get(2)).isEqualTo(event3);
    }

    @Test
    @DisplayName("빈 이벤트 리스트를 발행할 때 아무 동작도 하지 않는다")
    void testPublishEmptyEventList() {
        // Given
        List<DomainEvent> emptyEvents = Arrays.asList();

        // When
        eventPublisherAdapter.publishAll(emptyEvents);

        // Then
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("이벤트 발행 중 예외가 발생해도 전파된다")
    void testPublishEventWithException() {
        // Given
        RuntimeException expectedException = new RuntimeException("Event publishing failed");
        doThrow(expectedException).when(applicationEventPublisher).publishEvent(testEvent);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            eventPublisherAdapter.publish(testEvent);
        });
        assertEquals("Event publishing failed", thrown.getMessage());

        verify(applicationEventPublisher, times(1)).publishEvent(testEvent);
    }

    @Test
    @DisplayName("여러 이벤트 발행 중 하나가 실패하면 해당 지점에서 중단된다")
    void testPublishAllWithFailure() {
        // Given
        TestDomainEvent event1 = new TestDomainEvent("aggregate-1", "data-1");
        TestDomainEvent event2 = new TestDomainEvent("aggregate-2", "data-2");
        TestDomainEvent event3 = new TestDomainEvent("aggregate-3", "data-3");
        List<DomainEvent> events = Arrays.asList(event1, event2, event3);

        RuntimeException expectedException = new RuntimeException("Event publishing failed");
        doNothing().when(applicationEventPublisher).publishEvent(event1);
        doThrow(expectedException).when(applicationEventPublisher).publishEvent(event2);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            eventPublisherAdapter.publishAll(events);
        });
        assertEquals("Event publishing failed", thrown.getMessage());

        // event1은 발행되고, event2에서 실패, event3는 발행되지 않음
        verify(applicationEventPublisher, times(1)).publishEvent(event1);
        verify(applicationEventPublisher, times(1)).publishEvent(event2);
        verify(applicationEventPublisher, never()).publishEvent(event3);
    }

    // 테스트용 도메인 이벤트 구현
    private static class TestDomainEvent implements DomainEvent {
        private final String aggregateId;
        private final LocalDateTime occurredOn;
        private final String data;

        public TestDomainEvent(String aggregateId, String data) {
            this.aggregateId = aggregateId;
            this.data = data;
            this.occurredOn = LocalDateTime.now();
        }

        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public LocalDateTime getOccurredAt() {
            return occurredOn;
        }
        
        @Override
        public String eventType() {
            return "test.event";
        }

        public String getData() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestDomainEvent that = (TestDomainEvent) o;
            return aggregateId.equals(that.aggregateId) && data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return aggregateId.hashCode() + data.hashCode();
        }
    }
}