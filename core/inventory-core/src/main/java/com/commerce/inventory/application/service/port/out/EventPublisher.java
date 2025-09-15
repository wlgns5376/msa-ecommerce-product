package com.commerce.inventory.application.service.port.out;

import com.commerce.common.event.DomainEvent;
import java.util.List;

/**
 * 도메인 이벤트 발행을 위한 포트 인터페이스
 * 헥사고날 아키텍처에서 코어 레이어는 이 인터페이스에만 의존합니다.
 */
public interface EventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}