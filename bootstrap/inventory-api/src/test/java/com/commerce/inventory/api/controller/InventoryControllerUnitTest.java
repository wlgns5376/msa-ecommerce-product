package com.commerce.inventory.api.controller;

import com.commerce.inventory.api.dto.CreateSkuRequest;
import com.commerce.inventory.application.usecase.CreateSkuCommand;
import com.commerce.inventory.application.usecase.CreateSkuResponse;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CreateSkuUseCase createSkuUseCase;

    @InjectMocks
    private InventoryController inventoryController;

    private CreateSkuRequest validRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController).build();
        objectMapper = new ObjectMapper();
        
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
    }
}