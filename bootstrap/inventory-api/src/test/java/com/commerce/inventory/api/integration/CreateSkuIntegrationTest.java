package com.commerce.inventory.api.integration;

import com.commerce.inventory.api.dto.CreateSkuRequest;
import com.commerce.inventory.api.dto.CreateSkuResponseDto;
import com.commerce.inventory.application.usecase.CreateSkuResponse;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SKU 생성 통합 테스트")
class CreateSkuIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateSkuUseCase createSkuUseCase;

    @Test
    @DisplayName("POST /api/inventory/skus - SKU를 성공적으로 생성한다")
    void shouldCreateSkuSuccessfully() throws Exception {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU001")
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .weight(1.5)
                .weightUnit("KG")
                .volume(100.0)
                .volumeUnit("LITER")
                .build();

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

        given(createSkuUseCase.execute(any())).willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/inventory/skus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId()))
                .andExpect(jsonPath("$.code").value(response.getCode()))
                .andExpect(jsonPath("$.name").value(response.getName()));
    }
}