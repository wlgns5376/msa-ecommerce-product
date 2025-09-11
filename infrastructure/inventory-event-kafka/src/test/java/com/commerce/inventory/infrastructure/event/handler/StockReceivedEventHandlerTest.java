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
@DisplayName("StockReceivedEventHandler 테스트")
class StockReceivedEventHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    private StockReceivedEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StockReceivedEventHandler(objectMapper);
    }

    @Test
    @DisplayName("재고 입고 이벤트를 성공적으로 처리한다")
    void shouldHandleStockReceivedEventSuccessfully() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String skuId = "SKU-001";
        Integer quantity = 100;
        String warehouseId = "WH-001";
        String reason = "PURCHASE_ORDER";

        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", skuId);
        payload.put("quantity", quantity);
        payload.put("warehouseId", warehouseId);
        payload.put("reason", reason);

        String payloadJson = "{\"skuId\":\"SKU-001\",\"quantity\":100,\"warehouseId\":\"WH-001\",\"reason\":\"PURCHASE_ORDER\"}";

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("StockReceivedEvent")
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
        assertThat(eventType).isEqualTo("StockReceivedEvent");
    }

    @Test
    @DisplayName("빈 페이로드를 처리한다")
    void shouldHandleEmptyPayload() {
        // Given
        EventMessage eventMessage = EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("StockReceivedEvent")
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
                .eventType("StockReceivedEvent")
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
    @DisplayName("반품으로 인한 재고 입고를 처리한다")
    void shouldHandleReturnStockReceipt() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String skuId = "SKU-002";
        Integer quantity = 5;
        String warehouseId = "WH-002";
        String reason = "RETURN";

        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", skuId);
        payload.put("quantity", quantity);
        payload.put("warehouseId", warehouseId);
        payload.put("reason", reason);

        String payloadJson = "{\"skuId\":\"SKU-002\",\"quantity\":5,\"warehouseId\":\"WH-002\",\"reason\":\"RETURN\"}";

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("StockReceivedEvent")
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
        result.join();
    }

    @Test
    @DisplayName("다양한 창고 ID 형식을 처리한다")
    void shouldHandleVariousWarehouseFormats() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String skuId = "SKU-003";
        Integer quantity = 50;
        String warehouseId = "WAREHOUSE_CENTRAL_2024";
        String reason = "TRANSFER";

        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", skuId);
        payload.put("quantity", quantity);
        payload.put("warehouseId", warehouseId);
        payload.put("reason", reason);

        String payloadJson = String.format(
            "{\"skuId\":\"%s\",\"quantity\":%d,\"warehouseId\":\"%s\",\"reason\":\"%s\"}",
            skuId, quantity, warehouseId, reason
        );

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("StockReceivedEvent")
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
        result.join();
    }
}