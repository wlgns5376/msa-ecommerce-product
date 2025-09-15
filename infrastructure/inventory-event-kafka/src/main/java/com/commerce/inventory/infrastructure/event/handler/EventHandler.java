package com.commerce.inventory.infrastructure.event.handler;

import com.commerce.inventory.infrastructure.event.serialization.EventMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 이벤트 핸들러 인터페이스
 */
public interface EventHandler {
    
    /**
     * 이벤트를 처리합니다.
     * 
     * @param eventMessage 처리할 이벤트 메시지
     * @return 처리 완료 Future
     */
    CompletableFuture<Void> handle(EventMessage eventMessage);
    
    /**
     * 이 핸들러가 처리할 수 있는 이벤트 타입을 반환합니다.
     * 
     * @return 이벤트 타입
     */
    String getEventType();
}