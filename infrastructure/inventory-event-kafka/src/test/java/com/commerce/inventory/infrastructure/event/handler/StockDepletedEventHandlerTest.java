package com.commerce.inventory.infrastructure.event.handler;

import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockDepletedEventHandler 테스트")
class StockDepletedEventHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    private StockDepletedEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StockDepletedEventHandler(objectMapper);
    }

    @Test
    @DisplayName("재고 소진 이벤트를 성공적으로 처리한다")
    void shouldHandleStockDepletedEventSuccessfully() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String skuId = "SKU-001";

        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", skuId);

        String payloadJson = "{\"skuId\":\"SKU-001\"}";

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("StockDepletedEvent")
                .aggregateId(skuId)
                .aggregateType("SKU")
                .payload(payloadJson)
                .occurredAt(Instant.now())
                .build();

        when(objectMapper.readValue(eq(payloadJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(payload);

        // When
        CompletableFuture<Void> result = handler.handle(eventMessage);

        // Then
        assertThat(result).isNotNull();
        result.join(); // 완료 대기
    }

    @Test
    @DisplayName("이벤트 타입을 올바르게 반환한다")
    void shouldReturnCorrectEventType() {
        // When
        String eventType = handler.getEventType();

        // Then
        assertThat(eventType).isEqualTo("StockDepletedEvent");
    }

    @Test
    @DisplayName("빈 페이로드를 처리한다")
    void shouldHandleEmptyPayload() {
        // Given
        EventMessage eventMessage = EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("StockDepletedEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload(null)
                .occurredAt(Instant.now())
                .build();

        // When
        CompletableFuture<Void> result = handler.handle(eventMessage);

        // Then
        assertThat(result).isNotNull();
        result.join(); // 빈 페이로드에서도 예외 없이 처리되어야 함
    }

    @Test
    @DisplayName("페이로드 파싱 실패 시 예외를 던진다")
    void shouldThrowExceptionOnPayloadParsingFailure() throws Exception {
        // Given
        String payloadJson = "{invalid json}";

        EventMessage eventMessage = EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("StockDepletedEvent")
                .aggregateId("SKU-001")
                .aggregateType("SKU")
                .payload(payloadJson)
                .occurredAt(Instant.now())
                .build();

        when(objectMapper.readValue(eq(payloadJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new RuntimeException("Parsing failed"));

        // When & Then
        CompletableFuture<Void> result = handler.handle(eventMessage);
        assertThatThrownBy(result::join)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process event");
    }

    @Test
    @DisplayName("다양한 SKU ID 형식을 처리한다")
    void shouldHandleVariousSkuFormats() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String specialSkuId = "SKU_SPECIAL_2024_001";

        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", specialSkuId);

        String payloadJson = String.format("{\"skuId\":\"%s\"}", specialSkuId);

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("StockDepletedEvent")
                .aggregateId(specialSkuId)
                .aggregateType("SKU")
                .payload(payloadJson)
                .occurredAt(Instant.now())
                .build();

        when(objectMapper.readValue(eq(payloadJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(payload);

        // When
        CompletableFuture<Void> result = handler.handle(eventMessage);

        // Then
        assertThat(result).isNotNull();
        result.join();
    }

    @Test
    @DisplayName("여러 SKU에 대한 재고 소진 이벤트를 처리한다")
    void shouldHandleMultipleDepletedEvents() throws Exception {
        // Given - 첫 번째 SKU
        String eventId1 = UUID.randomUUID().toString();
        String skuId1 = "SKU-001";
        Map<String, Object> payload1 = new HashMap<>();
        payload1.put("skuId", skuId1);
        String payloadJson1 = "{\"skuId\":\"SKU-001\"}";

        EventMessage eventMessage1 = EventMessage.builder()
                .eventId(eventId1)
                .eventType("StockDepletedEvent")
                .aggregateId(skuId1)
                .aggregateType("SKU")
                .payload(payloadJson1)
                .occurredAt(Instant.now())
                .build();

        // Given - 두 번째 SKU
        String eventId2 = UUID.randomUUID().toString();
        String skuId2 = "SKU-002";
        Map<String, Object> payload2 = new HashMap<>();
        payload2.put("skuId", skuId2);
        String payloadJson2 = "{\"skuId\":\"SKU-002\"}";

        EventMessage eventMessage2 = EventMessage.builder()
                .eventId(eventId2)
                .eventType("StockDepletedEvent")
                .aggregateId(skuId2)
                .aggregateType("SKU")
                .payload(payloadJson2)
                .occurredAt(Instant.now())
                .build();

        when(objectMapper.readValue(eq(payloadJson1), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(payload1);
        when(objectMapper.readValue(eq(payloadJson2), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(payload2);

        // When
        CompletableFuture<Void> result1 = handler.handle(eventMessage1);
        CompletableFuture<Void> result2 = handler.handle(eventMessage2);

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        CompletableFuture.allOf(result1, result2).join();
    }
}