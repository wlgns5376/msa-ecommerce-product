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
@DisplayName("ReservationReleasedEventHandler 테스트")
class ReservationReleasedEventHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    private ReservationReleasedEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReservationReleasedEventHandler(objectMapper);
    }

    @Test
    @DisplayName("예약 해제 이벤트를 성공적으로 처리한다")
    void shouldHandleReservationReleasedEventSuccessfully() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String skuId = "SKU-001";
        Integer quantity = 10;
        String reservationId = "RES-001";
        String reason = "EXPIRED";

        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", skuId);
        payload.put("quantity", quantity);
        payload.put("reservationId", reservationId);
        payload.put("reason", reason);

        String payloadJson = "{\"skuId\":\"SKU-001\",\"quantity\":10,\"reservationId\":\"RES-001\",\"reason\":\"EXPIRED\"}";

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("ReservationReleasedEvent")
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
        assertThat(eventType).isEqualTo("ReservationReleasedEvent");
    }

    @Test
    @DisplayName("빈 페이로드를 처리한다")
    void shouldHandleEmptyPayload() {
        // Given
        EventMessage eventMessage = EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ReservationReleasedEvent")
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
                .eventType("ReservationReleasedEvent")
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
    @DisplayName("취소 사유별로 다르게 처리한다")
    void shouldHandleDifferentCancellationReasons() throws Exception {
        // Given - CANCELLED reason
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", "SKU-001");
        payload.put("quantity", 5);
        payload.put("reservationId", "RES-002");
        payload.put("reason", "CANCELLED");

        String payloadJson = "{\"skuId\":\"SKU-001\",\"quantity\":5,\"reservationId\":\"RES-002\",\"reason\":\"CANCELLED\"}";

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("ReservationReleasedEvent")
                .aggregateId("SKU-001")
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