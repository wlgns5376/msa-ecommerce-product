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
 * 상품 생성 이벤트 핸들러
 */
@Component
public class ProductCreatedEventHandler implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductCreatedEventHandler.class);
    private static final String EVENT_TYPE = "ProductCreatedEvent";
    
    private final ObjectMapper objectMapper;
    
    public ProductCreatedEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public CompletableFuture<Void> handle(EventMessage eventMessage) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Processing ProductCreatedEvent: eventId={}, aggregateId={}", 
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
                String productType = (String) payload.get("productType");
                String categoryId = (String) payload.get("categoryId");
                
                logger.info("New product created: productId={}, name={}, type={}, categoryId={}", 
                          productId, productName, productType, categoryId);
                
                // 실제 비즈니스 로직 처리
                processProductCreation(productId, productName, productType, categoryId);
                
                logger.info("Successfully processed ProductCreatedEvent: {}", eventMessage.getEventId());
                
            } catch (Exception e) {
                logger.error("Failed to process ProductCreatedEvent: {}", eventMessage.getEventId(), e);
                throw new RuntimeException("Failed to process event", e);
            }
        });
    }
    
    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
    
    private void processProductCreation(String productId, String productName, String productType, String categoryId) {
        // 실제 비즈니스 로직 구현
        // 예시:
        // - 검색 인덱스 업데이트
        // - 캐시 초기화
        // - 추천 시스템 업데이트
        // - 분석 데이터 수집
        
        logger.debug("Processing product creation business logic for: {}", productName);
        
        // 검색 인덱스 업데이트 (예시)
        updateSearchIndex(productId, productName, categoryId);
        
        // 추천 시스템 업데이트 (예시)
        updateRecommendationEngine(productId, productType);
        
        // 신상품 알림 (예시)
        if (isNewArrival(productType)) {
            notifyNewArrival(productId, productName);
        }
    }
    
    private void updateSearchIndex(String productId, String productName, String categoryId) {
        logger.info("Updating search index for new product: {}", productId);
        // 검색 인덱스 업데이트 로직
    }
    
    private void updateRecommendationEngine(String productId, String productType) {
        logger.info("Updating recommendation engine for product: {} of type: {}", productId, productType);
        // 추천 엔진 업데이트 로직
    }
    
    private boolean isNewArrival(String productType) {
        // 신상품 여부 판단 로직
        return true; // 예시
    }
    
    private void notifyNewArrival(String productId, String productName) {
        logger.info("Sending new arrival notification for product: {}", productName);
        // 신상품 알림 로직
    }
}