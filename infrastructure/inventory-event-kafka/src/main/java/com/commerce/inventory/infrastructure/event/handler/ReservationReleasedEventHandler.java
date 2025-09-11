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
 * 예약 해제 이벤트 핸들러
 */
@Component
public class ReservationReleasedEventHandler implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ReservationReleasedEventHandler.class);
    private static final String EVENT_TYPE = "ReservationReleasedEvent";
    
    private final ObjectMapper objectMapper;
    
    public ReservationReleasedEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public CompletableFuture<Void> handle(EventMessage eventMessage) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Processing ReservationReleasedEvent: eventId={}, aggregateId={}", 
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
                String reason = (String) payload.get("reason");
                
                logger.info("Reservation released: skuId={}, quantity={}, reservationId={}, reason={}", 
                          skuId, quantity, reservationId, reason);
                
                // 실제 비즈니스 로직 처리
                processReservationRelease(skuId, quantity, reservationId, reason);
                
                logger.info("Successfully processed ReservationReleasedEvent: {}", eventMessage.getEventId());
                
            } catch (Exception e) {
                logger.error("Failed to process ReservationReleasedEvent: {}", eventMessage.getEventId(), e);
                throw new RuntimeException("Failed to process event", e);
            }
        });
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    
    private void processReservationRelease(String skuId, Integer quantity, String reservationId, String reason) {
        // 실제 비즈니스 로직 구현
        // 예시:
        // - 재고 가용성 업데이트 알림
        // - 예약 취소 통계 업데이트
        // - 재고 재할당 프로세스 트리거
        // - 분석용 데이터 저장
        
        logger.debug("Processing reservation release business logic for SKU: {}, reason: {}", skuId, reason);
        
        // 특정 사유별 처리 (예시)
        if ("EXPIRED".equals(reason)) {
            logger.info("Reservation {} expired, quantity {} returned to available stock", reservationId, quantity);
        } else if ("CANCELLED".equals(reason)) {
            logger.info("Reservation {} was cancelled, quantity {} returned to available stock", reservationId, quantity);
        }
    }
}