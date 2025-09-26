package com.commerce.inventory.api.controller;

import com.commerce.inventory.api.dto.CreateSkuRequest;
import com.commerce.inventory.api.dto.ReserveStockRequest;
import com.commerce.inventory.application.service.port.out.*;
import com.commerce.inventory.application.usecase.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(InventoryControllerTest.TestConfig.class)
class InventoryControllerTest {
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public Clock clock() {
            return Clock.systemDefaultZone();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateSkuUseCase createSkuUseCase;
    
    @MockBean
    private ReserveStockUseCase reserveStockUseCase;
    
    @MockBean
    private ReleaseReservationUseCase releaseReservationUseCase;
    
    // CreateSkuService 의존성들을 Mock으로 추가
    @MockBean
    private LoadSkuPort loadSkuPort;
    
    @MockBean
    private SaveSkuPort saveSkuPort;
    
    @MockBean
    private LoadInventoryPort loadInventoryPort;
    
    @MockBean
    private SaveInventoryPort saveInventoryPort;
    
    @MockBean
    private LoadReservationPort loadReservationPort;
    
    @MockBean
    private SaveReservationPort saveReservationPort;
    
    @MockBean
    private SaveStockMovementPort saveStockMovementPort;
    
    @MockBean
    private EventPublisher eventPublisher;

    private CreateSkuRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CreateSkuRequest.builder()
                .code("SKU001")
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .weight(1.5)
                .weightUnit("KG")
                .volume(100.0)
                .volumeUnit("LITER")
                .build();
    }

    @Nested
    @DisplayName("POST /api/inventory/skus - SKU 생성")
    class CreateSku {

        @Test
        @DisplayName("유효한 요청으로 SKU를 성공적으로 생성한다")
        void shouldCreateSkuSuccessfully() throws Exception {
            // Given
            CreateSkuResponse response = CreateSkuResponse.builder()
                    .id("SKU-123456")
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit("KG")
                    .volume(100.0)
                    .volumeUnit("LITER")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(createSkuUseCase.execute(any(CreateSkuCommand.class)))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(response.getId()))
                    .andExpect(jsonPath("$.code").value(response.getCode()))
                    .andExpect(jsonPath("$.name").value(response.getName()))
                    .andExpect(jsonPath("$.description").value(response.getDescription()))
                    .andExpect(jsonPath("$.weight").value(response.getWeight()))
                    .andExpect(jsonPath("$.weightUnit").value(response.getWeightUnit()))
                    .andExpect(jsonPath("$.volume").value(response.getVolume()))
                    .andExpect(jsonPath("$.volumeUnit").value(response.getVolumeUnit()))
                    .andExpect(jsonPath("$.createdAt").exists());

            verify(createSkuUseCase).execute(any(CreateSkuCommand.class));
        }

        @Test
        @DisplayName("필수 필드가 없으면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenRequiredFieldIsMissing() throws Exception {
            // Given
            CreateSkuRequest invalidRequest = CreateSkuRequest.builder()
                    .code(null) // 필수 필드 누락
                    .name("테스트 상품")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("SKU 코드가 빈 문자열이면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenCodeIsEmpty() throws Exception {
            // Given
            CreateSkuRequest invalidRequest = CreateSkuRequest.builder()
                    .code("")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit("KG")
                    .volume(100.0)
                    .volumeUnit("LITER")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("무게가 음수이면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenWeightIsNegative() throws Exception {
            // Given
            CreateSkuRequest invalidRequest = CreateSkuRequest.builder()
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(-1.5)
                    .weightUnit("KG")
                    .volume(100.0)
                    .volumeUnit("LITER")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("부피가 음수이면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenVolumeIsNegative() throws Exception {
            // Given
            CreateSkuRequest invalidRequest = CreateSkuRequest.builder()
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit("KG")
                    .volume(-100.0)
                    .volumeUnit("LITER")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 무게 단위를 사용하면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenWeightUnitIsInvalid() throws Exception {
            // Given
            CreateSkuRequest invalidRequest = CreateSkuRequest.builder()
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit("INVALID_UNIT")
                    .volume(100.0)
                    .volumeUnit("LITER")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 부피 단위를 사용하면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenVolumeUnitIsInvalid() throws Exception {
            // Given
            CreateSkuRequest invalidRequest = CreateSkuRequest.builder()
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit("KG")
                    .volume(100.0)
                    .volumeUnit("INVALID_UNIT")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("무게와 무게 단위는 함께 제공되어야 한다")
        void shouldReturnBadRequestWhenWeightProvidedWithoutUnit() throws Exception {
            // Given
            CreateSkuRequest invalidRequest = CreateSkuRequest.builder()
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit(null) // 무게는 있지만 단위가 없음
                    .volume(100.0)
                    .volumeUnit("LITER")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("부피와 부피 단위는 함께 제공되어야 한다")
        void shouldReturnBadRequestWhenVolumeProvidedWithoutUnit() throws Exception {
            // Given
            CreateSkuRequest invalidRequest = CreateSkuRequest.builder()
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit("KG")
                    .volume(100.0)
                    .volumeUnit(null) // 부피는 있지만 단위가 없음
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/skus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/inventory/reservations - 재고 예약")
    class ReserveStock {

        @Test
        @DisplayName("유효한 요청으로 재고를 성공적으로 예약한다")
        void shouldReserveStockSuccessfully() throws Exception {
            // Given
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .orderId("ORDER-001")
                    .ttlSeconds(900)
                    .item(ReserveStockRequest.ReservationItem.builder()
                            .skuId("SKU-001")
                            .quantity(2)
                            .build())
                    .item(ReserveStockRequest.ReservationItem.builder()
                            .skuId("SKU-002")
                            .quantity(1)
                            .build())
                    .build();

            ReserveStockResponse response = ReserveStockResponse.builder()
                    .reservations(java.util.List.of(
                            ReserveStockResponse.ReservationResult.builder()
                                    .reservationId("RES-001")
                                    .skuId("SKU-001")
                                    .quantity(2)
                                    .expiresAt(LocalDateTime.now().plusSeconds(900))
                                    .status("RESERVED")
                                    .build(),
                            ReserveStockResponse.ReservationResult.builder()
                                    .reservationId("RES-002")
                                    .skuId("SKU-002")
                                    .quantity(1)
                                    .expiresAt(LocalDateTime.now().plusSeconds(900))
                                    .status("RESERVED")
                                    .build()
                    ))
                    .build();

            given(reserveStockUseCase.execute(any(ReserveStockCommand.class)))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(post("/api/inventory/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reservations").isArray())
                    .andExpect(jsonPath("$.reservations[0].reservationId").value("RES-001"))
                    .andExpect(jsonPath("$.reservations[0].skuId").value("SKU-001"))
                    .andExpect(jsonPath("$.reservations[0].quantity").value(2))
                    .andExpect(jsonPath("$.reservations[0].status").value("RESERVED"))
                    .andExpect(jsonPath("$.reservations[0].expiresAt").exists())
                    .andExpect(jsonPath("$.reservations[1].reservationId").value("RES-002"))
                    .andExpect(jsonPath("$.reservations[1].skuId").value("SKU-002"))
                    .andExpect(jsonPath("$.reservations[1].quantity").value(1))
                    .andExpect(jsonPath("$.reservations[1].status").value("RESERVED"));

            verify(reserveStockUseCase).execute(any(ReserveStockCommand.class));
        }

        @Test
        @DisplayName("주문 ID가 없으면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenOrderIdIsNull() throws Exception {
            // Given
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .orderId(null) // 주문 ID 누락
                    .item(ReserveStockRequest.ReservationItem.builder()
                            .skuId("SKU-001")
                            .quantity(2)
                            .build())
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("예약 항목이 없으면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenItemsIsEmpty() throws Exception {
            // Given
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .orderId("ORDER-001")
                    .build(); // items가 비어있음

            // When & Then
            mockMvc.perform(post("/api/inventory/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("SKU ID가 없으면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenSkuIdIsNull() throws Exception {
            // Given
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .orderId("ORDER-001")
                    .item(ReserveStockRequest.ReservationItem.builder()
                            .skuId(null) // SKU ID 누락
                            .quantity(2)
                            .build())
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("수량이 0 이하이면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenQuantityIsZeroOrNegative() throws Exception {
            // Given
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .orderId("ORDER-001")
                    .item(ReserveStockRequest.ReservationItem.builder()
                            .skuId("SKU-001")
                            .quantity(0) // 잘못된 수량
                            .build())
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TTL이 음수이면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenTtlIsNegative() throws Exception {
            // Given
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .orderId("ORDER-001")
                    .ttlSeconds(-1) // 음수 TTL
                    .item(ReserveStockRequest.ReservationItem.builder()
                            .skuId("SKU-001")
                            .quantity(2)
                            .build())
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TTL이 최대값을 초과하면 400 에러를 반환한다")
        void shouldReturnBadRequestWhenTtlExceedsMaximum() throws Exception {
            // Given
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .orderId("ORDER-001")
                    .ttlSeconds(86401) // 24시간 초과
                    .item(ReserveStockRequest.ReservationItem.builder()
                            .skuId("SKU-001")
                            .quantity(2)
                            .build())
                    .build();

            // When & Then
            mockMvc.perform(post("/api/inventory/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
    
    @Nested
    @DisplayName("DELETE /api/inventory/reservations/{id} - 재고 예약 취소")
    class ReleaseReservation {

        @BeforeEach
        void setUp() {
            // Reset mocks to ensure clean state
            Mockito.reset(releaseReservationUseCase);
        }

        @Test
        @DisplayName("유효한 예약 ID로 재고 예약을 성공적으로 취소한다")
        void shouldReleaseReservationSuccessfully() throws Exception {
            // Given
            String reservationId = "RES-001";
            doNothing().when(releaseReservationUseCase).release(any(ReleaseReservationCommand.class));

            // When & Then
            mockMvc.perform(delete("/api/inventory/reservations/{id}", reservationId))
                    .andExpect(status().isNoContent());

            verify(releaseReservationUseCase, Mockito.times(1)).release(any(ReleaseReservationCommand.class));
        }

        @Test
        @DisplayName("존재하지 않는 예약 ID로 요청하면 404 에러를 반환한다")
        void shouldReturnNotFoundWhenReservationDoesNotExist() throws Exception {
            // Given
            String reservationId = "INVALID-RES";
            doThrow(new com.commerce.inventory.domain.exception.ReservationNotFoundException("예약을 찾을 수 없습니다: " + reservationId))
                    .when(releaseReservationUseCase).release(any(ReleaseReservationCommand.class));

            // When & Then
            mockMvc.perform(delete("/api/inventory/reservations/{id}", reservationId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("빈 예약 ID로 요청하면 404 에러를 반환한다")
        void shouldReturnNotFoundWhenReservationIdIsEmpty() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/inventory/reservations/ "))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("이미 취소된 예약을 재시도하면 409 에러를 반환한다")
        void shouldReturnConflictWhenReservationAlreadyReleased() throws Exception {
            // Given
            String reservationId = "RES-001";
            doThrow(new com.commerce.inventory.domain.exception.ReservationAlreadyReleasedException("이미 취소된 예약입니다: " + reservationId))
                    .when(releaseReservationUseCase).release(any(ReleaseReservationCommand.class));

            // When & Then
            mockMvc.perform(delete("/api/inventory/reservations/{id}", reservationId))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("만료된 예약을 취소하려하면 409 에러를 반환한다")
        void shouldReturnConflictWhenReservationExpired() throws Exception {
            // Given
            String reservationId = "RES-001";
            doThrow(new com.commerce.inventory.domain.exception.ReservationExpiredException("만료된 예약입니다: " + reservationId))
                    .when(releaseReservationUseCase).release(any(ReleaseReservationCommand.class));

            // When & Then
            mockMvc.perform(delete("/api/inventory/reservations/{id}", reservationId))
                    .andExpect(status().isConflict());
        }
    }
}