package com.commerce.inventory.infrastructure.event.dlq;

import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Dead Letter Queue 서비스
 * 처리 실패한 메시지를 DLQ로 전송
 */
@Service
public class DeadLetterQueueService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueService.class);
    private static final String DLQ_TOPIC_PREFIX = "dlq-";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public DeadLetterQueueService(KafkaTemplate<String, Object> kafkaTemplate, 
                                  ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 실패한 이벤트를 DLQ로 전송
     */
    public void sendToDeadLetterQueue(EventMessage eventMessage, String originalTopic, 
                                      Throwable error, int retryCount) {
        try {
            String dlqTopic = DLQ_TOPIC_PREFIX + originalTopic;
            
            Map<String, Object> dlqMessage = new HashMap<>();
            dlqMessage.put("originalEvent", eventMessage);
            dlqMessage.put("originalTopic", originalTopic);
            dlqMessage.put("failureReason", error != null ? error.getMessage() : "Unknown error");
            dlqMessage.put("failureStackTrace", getStackTrace(error));
            dlqMessage.put("retryCount", retryCount);
            dlqMessage.put("failedAt", Instant.now().toString());
            
            kafkaTemplate.send(dlqTopic, eventMessage.getEventId(), dlqMessage)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send message to DLQ: eventId={}, topic={}", 
                                   eventMessage.getEventId(), dlqTopic, ex);
                    } else {
                        logger.info("Successfully sent message to DLQ: eventId={}, topic={}, retryCount={}", 
                                  eventMessage.getEventId(), dlqTopic, retryCount);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Error sending message to DLQ: eventId={}", eventMessage.getEventId(), e);
        }
    }
    
    /**
     * DLQ에서 메시지 재처리
     */
    public void reprocessFromDeadLetterQueue(String dlqTopic, String eventId) {
        try {
            logger.info("Reprocessing message from DLQ: topic={}, eventId={}", dlqTopic, eventId);
            
            String originalTopic = dlqTopic.replace(DLQ_TOPIC_PREFIX, "");
            
            // TODO: DLQ에서 메시지를 읽어 원래 토픽으로 재발행하는 로직 구현
            // 이 기능은 별도의 관리 도구나 API를 통해 수동으로 트리거되어야 함
            
            logger.info("Message reprocessing initiated: eventId={}", eventId);
            
        } catch (Exception e) {
            logger.error("Error reprocessing message from DLQ: eventId={}", eventId, e);
        }
    }
    
    /**
     * 스택 트레이스 문자열 변환
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        
        StackTraceElement[] elements = throwable.getStackTrace();
        int limit = Math.min(elements.length, 10); // 상위 10개 스택만 저장
        
        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(elements[i].toString()).append("\n");
        }
        
        if (elements.length > limit) {
            sb.append("\t... ").append(elements.length - limit).append(" more\n");
        }
        
        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(getStackTrace(throwable.getCause()));
        }
        
        return sb.toString();
    }
}