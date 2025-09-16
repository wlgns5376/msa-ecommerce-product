package com.commerce.inventory.api.controller;

import com.commerce.inventory.api.dto.CreateSkuRequest;
import com.commerce.inventory.application.service.port.out.*;
import com.commerce.inventory.application.usecase.CreateSkuCommand;
import com.commerce.inventory.application.usecase.CreateSkuResponse;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
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
}