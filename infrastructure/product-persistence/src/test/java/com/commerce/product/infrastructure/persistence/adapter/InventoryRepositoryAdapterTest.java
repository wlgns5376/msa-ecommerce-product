package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.common.domain.model.Quantity;
import com.commerce.product.domain.model.inventory.Inventory;
import com.commerce.product.domain.model.inventory.SkuId;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class InventoryRepositoryAdapterTest {
    
    private MockWebServer mockWebServer;
    private InventoryRepositoryAdapter adapter;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = mockWebServer.url("/").toString();
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
                
        adapter = new InventoryRepositoryAdapter(webClient);
        objectMapper = new ObjectMapper();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void getAvailableQuantity_성공() throws Exception {
        // given
        String skuId = "SKU001";
        Map<String, Object> responseBody = Map.of(
                "skuId", skuId,
                "totalQuantity", 100,
                "reservedQuantity", 20,
                "availableQuantity", 80
        );
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(responseBody)));
        
        // when
        int availableQuantity = adapter.getAvailableQuantity(skuId);
        
        // then
        assertThat(availableQuantity).isEqualTo(80);
        
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/inventory/" + skuId);
    }
    
    @Test
    void getAvailableQuantity_NotFound시_0반환() throws Exception {
        // given
        String skuId = "SKU001";
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404));
        
        // when
        int availableQuantity = adapter.getAvailableQuantity(skuId);
        
        // then
        assertThat(availableQuantity).isEqualTo(0);
    }
    
    @Test
    void getAvailableQuantity_서버오류시_0반환() throws Exception {
        // given
        String skuId = "SKU001";
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));
        
        // when
        int availableQuantity = adapter.getAvailableQuantity(skuId);
        
        // then
        assertThat(availableQuantity).isEqualTo(0);
    }
    
    @Test
    void reserveStock_성공() throws Exception {
        // given
        String skuId = "SKU001";
        int quantity = 5;
        String orderId = "ORDER001";
        String expectedReservationId = "RESERVATION001";
        
        Map<String, Object> responseBody = Map.of(
                "reservationId", expectedReservationId,
                "skuId", skuId,
                "quantity", quantity,
                "orderId", orderId,
                "expiresAt", "2024-01-01T10:00:00Z"
        );
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(responseBody)));
        
        // when
        String reservationId = adapter.reserveStock(skuId, quantity, orderId);
        
        // then
        assertThat(reservationId).isEqualTo(expectedReservationId);
        
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/inventory/reservations");
        
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"skuId\":\"" + skuId + "\"");
        assertThat(requestBody).contains("\"quantity\":" + quantity);
        assertThat(requestBody).contains("\"orderId\":\"" + orderId + "\"");
        assertThat(requestBody).contains("\"ttl\":300");
    }
    
    @Test
    void reserveStock_실패시_예외발생() {
        // given
        String skuId = "SKU001";
        int quantity = 5;
        String orderId = "ORDER001";
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));
        
        // when & then
        assertThatThrownBy(() -> adapter.reserveStock(skuId, quantity, orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("재고 예약 중 오류가 발생했습니다.");
    }
    
    @Test
    void releaseReservation_성공() throws Exception {
        // given
        String reservationId = "RESERVATION001";
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));
        
        // when & then
        assertThatNoException().isThrownBy(() -> adapter.releaseReservation(reservationId));
        
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/api/inventory/reservations/" + reservationId);
    }
    
    @Test
    void releaseReservation_실패시_예외발생하지않음() {
        // given
        String reservationId = "RESERVATION001";
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));
        
        // when & then
        assertThatNoException().isThrownBy(() -> adapter.releaseReservation(reservationId));
    }
    
    @Test
    void findBySkuId_성공() throws Exception {
        // given
        SkuId skuId = new SkuId("SKU001");
        Map<String, Object> responseBody = Map.of(
                "skuId", skuId.value(),
                "totalQuantity", 100,
                "reservedQuantity", 20,
                "availableQuantity", 80
        );
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(responseBody)));
        
        // when
        Optional<Inventory> result = adapter.findBySkuId(skuId);
        
        // then
        assertThat(result).isPresent();
        Inventory inventory = result.get();
        assertThat(inventory.getAvailableQuantity()).isEqualTo(Quantity.of(80));
    }
    
    @Test
    void findBySkuId_NotFound시_빈Optional반환() throws Exception {
        // given
        SkuId skuId = new SkuId("SKU001");
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404));
        
        // when
        Optional<Inventory> result = adapter.findBySkuId(skuId);
        
        // then
        assertThat(result).isEmpty();
    }
    
    @Test
    void save_호출시_예외발생() {
        // given
        Inventory inventory = new TestInventory();
        
        // when & then
        assertThatThrownBy(() -> adapter.save(inventory))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Product service cannot directly save inventory");
    }
    
    private static class TestInventory implements Inventory {
        @Override
        public Quantity getAvailableQuantity() {
            return Quantity.of(100);
        }
    }
    
    @Test
    void findBySkuIds_성공() throws Exception {
        // given
        List<SkuId> skuIds = Arrays.asList(
                new SkuId("SKU001"),
                new SkuId("SKU002")
        );
        
        Map<String, Object> response1 = Map.of(
                "skuId", "SKU001",
                "totalQuantity", 100,
                "reservedQuantity", 20,
                "availableQuantity", 80
        );
        
        Map<String, Object> response2 = Map.of(
                "skuId", "SKU002",
                "totalQuantity", 50,
                "reservedQuantity", 10,
                "availableQuantity", 40
        );
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(response1)));
                
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(response2)));
        
        // when
        Map<SkuId, Inventory> result = adapter.findBySkuIds(skuIds);
        
        // then
        assertThat(result).hasSize(2);
        
        Inventory inventory1 = result.get(new SkuId("SKU001"));
        assertThat(inventory1).isNotNull();
        assertThat(inventory1.getAvailableQuantity()).isEqualTo(Quantity.of(80));
        
        Inventory inventory2 = result.get(new SkuId("SKU002"));
        assertThat(inventory2).isNotNull();
        assertThat(inventory2.getAvailableQuantity()).isEqualTo(Quantity.of(40));
    }
    
    @Test
    void findBySkuIds_빈리스트시_빈맵반환() {
        // when
        Map<SkuId, Inventory> result = adapter.findBySkuIds(List.of());
        
        // then
        assertThat(result).isEmpty();
    }
    
    @Test
    void findBySkuIds_null리스트시_빈맵반환() {
        // when
        Map<SkuId, Inventory> result = adapter.findBySkuIds(null);
        
        // then
        assertThat(result).isEmpty();
    }
    
    @Test
    void findBySkuIds_일부실패시_성공한것만반환() throws Exception {
        // given
        List<SkuId> skuIds = Arrays.asList(
                new SkuId("SKU001"),
                new SkuId("SKU002")
        );
        
        Map<String, Object> response1 = Map.of(
                "skuId", "SKU001",
                "totalQuantity", 100,
                "reservedQuantity", 20,
                "availableQuantity", 80
        );
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(response1)));
                
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)); // SKU002는 서버 오류
        
        // when
        Map<SkuId, Inventory> result = adapter.findBySkuIds(skuIds);
        
        // then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(new SkuId("SKU001"));
        assertThat(result).doesNotContainKey(new SkuId("SKU002"));
    }
}