package com.commerce.inventory.infrastructure.event.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

/**
 * Kafka Consumer 에러 핸들러
 */
public class KafkaConsumerErrorHandler implements CommonErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerErrorHandler.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    @Override
    public boolean handleOne(Exception exception, ConsumerRecord<?, ?> record, 
                            Consumer<?, ?> consumer, MessageListenerContainer container) {
        logger.error("Error processing record: topic={}, partition={}, offset={}, key={}", 
                    record.topic(), record.partition(), record.offset(), record.key(), exception);
        
        // 재시도 로직 구현
        Integer retryCount = getRetryCount(record);
        
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            logger.info("Retrying record processing. Attempt {} of {}", retryCount + 1, MAX_RETRY_ATTEMPTS);
            // 재시도를 위해 false 반환 (처리 계속)
            return false;
        } else {
            logger.error("Max retry attempts reached for record. Sending to DLQ or skipping.");
            sendToDeadLetterQueue(record, exception);
            // true 반환하여 다음 레코드로 이동
            return true;
        }
    }
    
    @Override
    public void handleOtherException(Exception exception, Consumer<?, ?> consumer,
                                    MessageListenerContainer container, boolean batchListener) {
        logger.error("Non-record exception occurred in consumer", exception);
        
        // 심각한 에러의 경우 컨테이너 중지 고려
        if (isSerious(exception)) {
            logger.error("Serious error detected. Stopping container.");
            container.stop();
        }
    }
    
    private Integer getRetryCount(ConsumerRecord<?, ?> record) {
        // 실제 구현에서는 헤더에서 재시도 횟수를 읽어올 수 있음
        // 여기서는 간단히 0을 반환
        return 0;
    }
    
    private void sendToDeadLetterQueue(ConsumerRecord<?, ?> record, Exception exception) {
        // DLQ로 메시지 전송 로직
        logger.info("Sending failed record to DLQ: topic={}, partition={}, offset={}", 
                   record.topic(), record.partition(), record.offset());
        
        // 실제 구현에서는 DLQ 토픽으로 메시지를 발행
        // kafkaTemplate.send("dlq-" + record.topic(), record.key(), record.value());
    }
    
    private boolean isSerious(Exception exception) {
        // 심각한 에러 판단 로직
        if (exception.getCause() instanceof OutOfMemoryError || 
            exception.getCause() instanceof StackOverflowError) {
            return true;
        }
        return exception.getMessage() != null && exception.getMessage().contains("Connection refused");
    }
}