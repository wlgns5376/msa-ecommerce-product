package com.commerce.inventory.infrastructure.event.kafka;

import com.commerce.common.event.DomainEvent;
import com.commerce.inventory.infrastructure.event.kafka.retry.RetryableEventStore;
import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import com.commerce.inventory.infrastructure.event.serialization.EventSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventPublisher 테스트")
class KafkaEventPublisherTest {
    
    @Mock
    private KafkaTemplate<String, EventMessage> kafkaTemplate;
    
    @Mock
    private EventSerializer eventSerializer;
    
    @Mock
    private KafkaTopicResolver topicResolver;
    
    @Mock
    private KafkaErrorHandler errorHandler;
    
    @Mock
    private RetryableEventStore retryableEventStore;
    
    private KafkaEventPublisher eventPublisher;
    
    @BeforeEach
    void setUp() {
        eventPublisher = new KafkaEventPublisher(
            kafkaTemplate,
            eventSerializer,
            topicResolver,
            errorHandler,
            retryableEventStore
        );
        eventPublisher.init();
    }
    
    @Test
    @DisplayName("도메인 이벤트를 성공적으로 발행한다")
    void testPublish_Success() {
        // Given
        TestDomainEvent event = new TestDomainEvent("aggregate-123");
        String topic = "test-topic";
        EventMessage message = createEventMessage();
        
        when(topicResolver.resolveTopic(event)).thenReturn(topic);
        when(eventSerializer.serialize(event)).thenReturn(message);
        
        CompletableFuture<SendResult<String, EventMessage>> future = 
            CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq(topic), any(String.class), eq(message)))
            .thenReturn(future);
        
        // When
        eventPublisher.publish(event);
        
        // Then
        verify(topicResolver).resolveTopic(event);
        verify(eventSerializer).serialize(event);
        verify(kafkaTemplate).send(eq(topic), eq("aggregate-123"), eq(message));
        verify(errorHandler, never()).handleError(any(), any());
    }
    
    @Test
    @DisplayName("이벤트 발행 실패 시 에러 핸들러를 호출한다")
    void testPublish_Failure() {
        // Given
        TestDomainEvent event = new TestDomainEvent("aggregate-123");
        String topic = "test-topic";
        EventMessage message = createEventMessage();
        RuntimeException error = new RuntimeException("Kafka error");
        
        when(topicResolver.resolveTopic(event)).thenReturn(topic);
        when(eventSerializer.serialize(event)).thenReturn(message);
        
        CompletableFuture<SendResult<String, EventMessage>> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        when(kafkaTemplate.send(eq(topic), any(String.class), eq(message)))
            .thenReturn(future);
        
        // When
        eventPublisher.publish(event);
        
        // Then
        verify(kafkaTemplate).send(eq(topic), eq("aggregate-123"), eq(message));
        verify(errorHandler, timeout(1000)).handleError(eq(event), any(Throwable.class));
    }
    
    @Test
    @DisplayName("여러 이벤트를 순차적으로 발행한다")
    void testPublishAll() {
        // Given
        TestDomainEvent event1 = new TestDomainEvent("aggregate-1");
        TestDomainEvent event2 = new TestDomainEvent("aggregate-2");
        List<DomainEvent> events = Arrays.asList(event1, event2);
        
        when(topicResolver.resolveTopic(any())).thenReturn("test-topic");
        when(eventSerializer.serialize(any())).thenReturn(createEventMessage());
        
        CompletableFuture<SendResult<String, EventMessage>> future = 
            CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(EventMessage.class)))
            .thenReturn(future);
        
        // When
        eventPublisher.publishAll(events);
        
        // Then
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any(EventMessage.class));
        verify(eventSerializer, times(2)).serialize(any(DomainEvent.class));
    }
    
    @Test
    @DisplayName("Aggregate 이벤트의 경우 aggregateId를 키로 사용한다")
    void testPublish_WithAggregateEvent() {
        // Given
        TestAggregateEvent event = new TestAggregateEvent("aggregate-456");
        String topic = "test-topic";
        EventMessage message = createEventMessage();
        
        when(topicResolver.resolveTopic(event)).thenReturn(topic);
        when(eventSerializer.serialize(event)).thenReturn(message);
        
        CompletableFuture<SendResult<String, EventMessage>> future = 
            CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq(topic), eq("aggregate-456"), eq(message)))
            .thenReturn(future);
        
        // When
        eventPublisher.publish(event);
        
        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(topic), keyCaptor.capture(), eq(message));
        assertThat(keyCaptor.getValue()).isEqualTo("aggregate-456");
    }
    
    @Test
    @DisplayName("직렬화 실패 시 에러 핸들러를 호출한다")
    void testPublish_SerializationFailure() {
        // Given
        TestDomainEvent event = new TestDomainEvent("aggregate-123");
        RuntimeException error = new RuntimeException("Serialization error");
        
        when(topicResolver.resolveTopic(event)).thenReturn("test-topic");
        when(eventSerializer.serialize(event)).thenThrow(error);
        
        // When
        eventPublisher.publish(event);
        
        // Then
        verify(errorHandler).handleError(event, error);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(EventMessage.class));
    }
    
    private EventMessage createEventMessage() {
        return EventMessage.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("TestEvent")
            .occurredAt(LocalDateTime.now())
            .build();
    }
    
    // Test event classes
    static class TestDomainEvent implements DomainEvent {
        private final String id;
        private final LocalDateTime occurredAt = LocalDateTime.now();
        
        TestDomainEvent(String id) {
            this.id = id;
        }
        
        @Override
        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }
        
        @Override
        public String eventType() {
            return "TestDomainEvent";
        }
    }
    
    static class TestAggregateEvent extends TestDomainEvent implements AggregateEvent {
        private final String aggregateId;
        
        TestAggregateEvent(String aggregateId) {
            super(aggregateId);
            this.aggregateId = aggregateId;
        }
        
        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }
}