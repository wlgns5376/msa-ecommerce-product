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
 * 재고 예약 이벤트 핸들러
 */
@Component
public class StockReservedEventHandler implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StockReservedEventHandler.class);
    private static final String EVENT_TYPE = "StockReservedEvent";
    
    private final ObjectMapper objectMapper;
    
    public StockReservedEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public CompletableFuture<Void> handle(EventMessage eventMessage) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Processing StockReservedEvent: eventId={}, aggregateId={}", 
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
                Integer quantity = (Integer) payload.get("quantity");
                String reservationId = (String) payload.get("reservationId");
                
                logger.info("Stock reserved: skuId={}, quantity={}, reservationId={}", 
                          skuId, quantity, reservationId);
                
                // 실제 비즈니스 로직 처리
                // 예: 알림 발송, 대시보드 업데이트, 분석 데이터 저장 등
                processStockReservation(skuId, quantity, reservationId);
                
                logger.info("Successfully processed StockReservedEvent: {}", eventMessage.getEventId());
                
            } catch (Exception e) {
                logger.error("Failed to process StockReservedEvent: {}", eventMessage.getEventId(), e);
                throw new RuntimeException("Failed to process event", e);
            }
        });
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    
    private void processStockReservation(String skuId, Integer quantity, String reservationId) {
        // 실제 비즈니스 로직 구현
        // 예시:
        // - 재고 부족 알림 확인
        // - 재주문 필요성 확인
        // - 실시간 대시보드 업데이트
        // - 분석용 데이터 저장
        
        logger.debug("Processing stock reservation business logic for SKU: {}", skuId);
        
        // 재고 부족 체크 (예시)
        if (quantity > 100) {
            logger.warn("Large quantity reserved for SKU {}: {} units", skuId, quantity);
        }
    }
}