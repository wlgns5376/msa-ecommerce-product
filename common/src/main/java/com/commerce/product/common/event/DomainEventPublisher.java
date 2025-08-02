package com.commerce.product.common.event;

/**
 * 도메인 이벤트 발행을 위한 인터페이스
 * 인프라스트럭처 계층에서 구현체를 제공합니다.
 */
public interface DomainEventPublisher {
    
    /**
     * 도메인 이벤트를 발행합니다.
     * 
     * @param event 발행할 도메인 이벤트
     */
    void publish(DomainEvent event);
}