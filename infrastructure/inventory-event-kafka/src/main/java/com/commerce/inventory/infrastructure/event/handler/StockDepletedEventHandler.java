package com.commerce.inventory.infrastructure.event.handler;

import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 재고 소진 이벤트 핸들러
 */
@Component
public class StockDepletedEventHandler implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StockDepletedEventHandler.class);
    private static final String EVENT_TYPE = "StockDepletedEvent";
    
    private final ObjectMapper objectMapper;
    
    public StockDepletedEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public CompletableFuture<Void> handle(EventMessage eventMessage) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Processing StockDepletedEvent: eventId={}, aggregateId={}", 
                          eventMessage.getEventId(), eventMessage.getAggregateId());
                
                // 페이로드 파싱
                String payloadStr = eventMessage.getPayload();
                if (payloadStr == null || payloadStr.isEmpty()) {
                    logger.warn("Empty payload for event: {}", eventMessage.getEventId());
                    return;
                }
                
                Map<String, Object> payload = objectMapper.readValue(
                    payloadStr, 
                    new TypeReference<Map<String, Object>>() {}
                );
                
                String skuId = (String) payload.get("skuId");
                
                logger.warn("Stock depleted for SKU: {}", skuId);
                
                // 실제 비즈니스 로직 처리
                processStockDepletion(skuId);
                
                logger.info("Successfully processed StockDepletedEvent: {}", eventMessage.getEventId());
                
            } catch (Exception e) {
                logger.error("Failed to process StockDepletedEvent: {}", eventMessage.getEventId(), e);
                throw new RuntimeException("Failed to process event", e);
            }
        });
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    
    private void processStockDepletion(String skuId) {
        // 실제 비즈니스 로직 구현
        // 예시:
        // - 긴급 재입고 알림 발송
        // - 자동 재주문 프로세스 트리거
        // - 관련 상품 판매 중지
        // - 대시보드 경고 표시
        // - 구매팀에 알림 발송
        
        logger.warn("URGENT: Stock depleted for SKU: {}. Triggering replenishment process.", skuId);
        
        // 긴급 알림 발송 (예시)
        sendUrgentNotification(skuId);
        
        // 자동 재주문 프로세스 (예시)
        triggerAutoReplenishment(skuId);
        
        // 상품 상태 업데이트 (예시)
        updateProductAvailability(skuId, false);
    }
    
    private void sendUrgentNotification(String skuId) {
        logger.info("Sending urgent notification for depleted stock: SKU={}", skuId);
        // 실제 알림 발송 로직
    }
    
    private void triggerAutoReplenishment(String skuId) {
        logger.info("Triggering auto-replenishment for SKU: {}", skuId);
        // 자동 재주문 로직
    }
    
    private void updateProductAvailability(String skuId, boolean available) {
        logger.info("Updating product availability for SKU: {} to {}", skuId, available);
        // 상품 가용성 업데이트 로직
    }
}