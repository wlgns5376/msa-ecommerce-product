package com.commerce.product.infrastructure.kafka.adapter;

import com.commerce.product.common.event.DomainEvent;
import com.commerce.product.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka를 통한 도메인 이벤트 발행 어댑터
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${spring.kafka.bootstrap-servers:}')")
public class DomainEventPublisherAdapter implements DomainEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publish(DomainEvent event) {
        try {
            String topic = generateTopicName(event.eventType());
            kafkaTemplate.send(topic, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("도메인 이벤트 발행 실패: {}", event.eventType(), ex);
                        } else {
                            log.debug("도메인 이벤트 발행 성공: {} -> {}", event.eventType(), topic);
                        }
                    });
        } catch (Exception e) {
            log.error("도메인 이벤트 발행 중 예외 발생: {}", event.eventType(), e);
        }
    }
    
    /**
     * 이벤트 타입을 기반으로 토픽명을 생성합니다.
     */
    private String generateTopicName(String eventType) {
        // 예: AccountCreatedEvent -> account.created
        return eventType.replaceAll("Event$", "")
                .replaceAll("([a-z])([A-Z])", "$1.$2")
                .toLowerCase();
    }
}