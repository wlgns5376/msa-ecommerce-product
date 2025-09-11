package com.commerce.inventory.infrastructure.event.handler;

import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@DisplayName("ProductCreatedEventHandler 테스트")
class ProductCreatedEventHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    private ProductCreatedEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductCreatedEventHandler(objectMapper);
    }

    @Test
    @DisplayName("상품 생성 이벤트를 성공적으로 처리한다")
    void shouldHandleProductCreatedEventSuccessfully() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String productId = "PROD-001";
        String productName = "테스트 상품";
        String productType = "PHYSICAL";
        String categoryId = "CAT-001";
        Double price = 50000.0;

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("productName", productName);
        payload.put("productType", productType);
        payload.put("categoryId", categoryId);
        payload.put("price", price);

        String payloadJson = "{\"productId\":\"PROD-001\",\"productName\":\"테스트 상품\",\"productType\":\"PHYSICAL\",\"categoryId\":\"CAT-001\",\"price\":50000.0}";

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("ProductCreatedEvent")
                .aggregateId(productId)
                .aggregateType("Product")
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
        assertThat(eventType).isEqualTo("ProductCreatedEvent");
    }

    @Test
    @DisplayName("빈 페이로드를 처리한다")
    void shouldHandleEmptyPayload() {
        // Given
        EventMessage eventMessage = EventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ProductCreatedEvent")
                .aggregateId("PROD-001")
                .aggregateType("Product")
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
                .eventType("ProductCreatedEvent")
                .aggregateId("PROD-001")
                .aggregateType("Product")
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

    @ParameterizedTest
    @DisplayName("다양한 상품 타입을 처리한다")
    @ValueSource(strings = {"PHYSICAL", "DIGITAL", "SERVICE", "SUBSCRIPTION"})
    void shouldHandleVariousProductTypes(String productType) throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String productId = "PROD-" + productType;
        String productName = productType + " 상품";
        String categoryId = "CAT-001";
        Double price = 30000.0;

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("productName", productName);
        payload.put("productType", productType);
        payload.put("categoryId", categoryId);
        payload.put("price", price);

        String payloadJson = String.format(
            "{\"productId\":\"%s\",\"productName\":\"%s\",\"productType\":\"%s\",\"categoryId\":\"%s\",\"price\":%.1f}",
            productId, productName, productType, categoryId, price
        );

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("ProductCreatedEvent")
                .aggregateId(productId)
                .aggregateType("Product")
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
    @DisplayName("다양한 가격대의 상품을 처리한다")
    void shouldHandleVariousPriceRanges() throws Exception {
        // Given - 고가 상품
        String eventId = UUID.randomUUID().toString();
        String productId = "PROD-PREMIUM";
        String productName = "프리미엄 상품";
        String productType = "PHYSICAL";
        String categoryId = "CAT-PREMIUM";
        Double highPrice = 5000000.0; // 500만원

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("productName", productName);
        payload.put("productType", productType);
        payload.put("categoryId", categoryId);
        payload.put("price", highPrice);

        String payloadJson = String.format(
            "{\"productId\":\"%s\",\"productName\":\"%s\",\"productType\":\"%s\",\"categoryId\":\"%s\",\"price\":%.1f}",
            productId, productName, productType, categoryId, highPrice
        );

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("ProductCreatedEvent")
                .aggregateId(productId)
                .aggregateType("Product")
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
    @DisplayName("복잡한 상품명과 카테고리를 처리한다")
    void shouldHandleComplexProductNamesAndCategories() throws Exception {
        // Given
        String eventId = UUID.randomUUID().toString();
        String productId = "PROD-SPECIAL-2024";
        String productName = "[2024 신상] 특별 한정판 상품 - Special Edition (Ver.2)";
        String productType = "PHYSICAL";
        String categoryId = "CAT/SUB/SPECIAL-2024";
        Double price = 99000.0;

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("productName", productName);
        payload.put("productType", productType);
        payload.put("categoryId", categoryId);
        payload.put("price", price);

        String payloadJson = String.format(
            "{\"productId\":\"%s\",\"productName\":\"%s\",\"productType\":\"%s\",\"categoryId\":\"%s\",\"price\":%.1f}",
            productId, productName, productType, categoryId, price
        );

        EventMessage eventMessage = EventMessage.builder()
                .eventId(eventId)
                .eventType("ProductCreatedEvent")
                .aggregateId(productId)
                .aggregateType("Product")
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