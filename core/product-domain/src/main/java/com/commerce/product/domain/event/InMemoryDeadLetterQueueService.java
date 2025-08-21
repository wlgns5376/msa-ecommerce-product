package com.commerce.product.domain.event;

import com.commerce.common.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class InMemoryDeadLetterQueueService implements DeadLetterQueueService {
    
    private final Map<String, FailedEventRecord> deadLetterQueue = new ConcurrentHashMap<>();
    
    @Value("${event.publication.retry.max-attempts:3}")
    private int maxAttempts;
    
    @Override
    public void storeFailedEvent(DomainEvent event, Exception failureReason) {
        String eventId = UUID.randomUUID().toString();
        String stackTrace = getStackTraceAsString(failureReason);
        
        FailedEventRecord record = new FailedEventRecord(
            eventId,
            event,
            event.getClass().getSimpleName(),
            LocalDateTime.now(),
            maxAttempts, // maxAttempts from configuration
            failureReason.getMessage(),
            stackTrace
        );
        
        deadLetterQueue.put(eventId, record);
        log.warn("Event stored in Dead Letter Queue. EventId: {}, Type: {}", 
            eventId, event.getClass().getSimpleName());
        
        // TODO: 프로덕션 환경에서는 다음 구현이 필요합니다:
        // 1. 영구 저장소(Redis, Database)에 저장
        // 2. 모니터링 메트릭 전송
        // 3. 알림 시스템 통합 (임계값 초과 시)
    }
    
    @Override
    public void processDeadLetterQueue() {
        // TODO: 배치 처리 로직 구현
        // 1. DLQ에서 이벤트 조회
        // 2. 재처리 가능 여부 판단
        // 3. 재처리 시도
        // 4. 성공 시 DLQ에서 제거
        // 5. 실패 시 재시도 횟수 증가 및 다음 처리 시간 설정
        log.info("Processing Dead Letter Queue. Current size: {}", deadLetterQueue.size());
    }
    
    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}