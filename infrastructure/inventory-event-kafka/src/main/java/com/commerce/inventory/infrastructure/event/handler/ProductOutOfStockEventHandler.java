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
 * 상품 품절 이벤트 핸들러
 */
@Component
public class ProductOutOfStockEventHandler implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductOutOfStockEventHandler.class);
    private static final String EVENT_TYPE = "ProductOutOfStockEvent";
    
    private final ObjectMapper objectMapper;
    
    public ProductOutOfStockEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public CompletableFuture<Void> handle(EventMessage eventMessage) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Processing ProductOutOfStockEvent: eventId={}, aggregateId={}", 
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
                
                String productId = (String) payload.get("productId");
                String productName = (String) payload.get("productName");
                
                logger.warn("Product out of stock: productId={}, productName={}", productId, productName);
                
                // 실제 비즈니스 로직 처리
                processProductOutOfStock(productId, productName);
                
                logger.info("Successfully processed ProductOutOfStockEvent: {}", eventMessage.getEventId());
                
            } catch (Exception e) {
                logger.error("Failed to process ProductOutOfStockEvent: {}", eventMessage.getEventId(), e);
                throw new RuntimeException("Failed to process event", e);
            }
        });
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    
    private void processProductOutOfStock(String productId, String productName) {
        // 실제 비즈니스 로직 구현
        // 예시:
        // - 고객에게 품절 알림 발송
        // - 상품 목록에서 품절 표시
        // - 대체 상품 추천
        // - 재입고 알림 신청 관리
        
        logger.info("Processing out of stock for product: {} ({})", productName, productId);
        
        // 고객 알림 (예시)
        notifyCustomersAboutOutOfStock(productId, productName);
        
        // 대체 상품 추천 프로세스 (예시)
        suggestAlternativeProducts(productId);
    }
    
    private void notifyCustomersAboutOutOfStock(String productId, String productName) {
        logger.info("Notifying customers about out of stock product: {}", productName);
        // 실제 알림 로직
    }
    
    private void suggestAlternativeProducts(String productId) {
        logger.info("Finding alternative products for: {}", productId);
        // 대체 상품 추천 로직
    }
}