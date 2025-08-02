package com.commerce.product.common.event;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트의 기본 인터페이스
 * 모든 도메인 이벤트는 이 인터페이스를 구현해야 합니다.
 */
public interface DomainEvent {
    
    /**
     * 이벤트 발생 시간을 반환합니다.
     */
    LocalDateTime occurredAt();
    
    /**
     * 이벤트 타입을 반환합니다.
     */
    String eventType();
}