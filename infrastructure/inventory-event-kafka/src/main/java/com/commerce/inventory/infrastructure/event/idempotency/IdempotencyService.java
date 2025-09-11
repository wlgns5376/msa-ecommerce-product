package com.commerce.inventory.infrastructure.event.idempotency;

/**
 * 이벤트 처리 멱등성을 보장하는 서비스
 */
public interface IdempotencyService {
    
    /**
     * 이벤트가 이미 처리되었는지 확인합니다.
     * 
     * @param eventId 이벤트 ID
     * @return 처리 여부
     */
    boolean isProcessed(String eventId);
    
    /**
     * 이벤트를 처리됨으로 표시합니다.
     * 
     * @param eventId 이벤트 ID
     */
    void markAsProcessed(String eventId);
}