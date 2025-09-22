package com.commerce.inventory.api.integration;

import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.domain.repository.SkuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SKU 조회 통합 테스트")
class GetSkuByIdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkuRepository skuRepository;

    private Sku testSku;
    private LocalDateTime currentTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();
        testSku = Sku.restore(
            SkuId.of("SKU-001"),
            SkuCode.of("TEST-SKU-001"),
            "테스트 SKU",
            "테스트용 SKU 설명입니다",
            Weight.of(1.5, WeightUnit.KILOGRAM),
            Volume.of(10.0, VolumeUnit.CUBIC_M),
            currentTime.minusDays(1),
            currentTime.minusDays(1),
            1L
        );
    }

    @Test
    @DisplayName("GET /api/inventory/skus/{id} - SKU를 성공적으로 조회한다")
    void should_get_sku_by_id_successfully() throws Exception {
        // Given
        String skuId = "SKU-001";
        when(skuRepository.findById(SkuId.of(skuId))).thenReturn(Optional.of(testSku));

        // When & Then
        mockMvc.perform(get("/api/inventory/skus/{id}", skuId)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(skuId))
            .andExpect(jsonPath("$.code").value("TEST-SKU-001"))
            .andExpect(jsonPath("$.name").value("테스트 SKU"))
            .andExpect(jsonPath("$.description").value("테스트용 SKU 설명입니다"))
            .andExpect(jsonPath("$.weight").value(1.5))
            .andExpect(jsonPath("$.volume").value(10.0))
            .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("GET /api/inventory/skus/{id} - 존재하지 않는 SKU 조회 시 404를 반환한다")
    void should_return_404_when_sku_not_found() throws Exception {
        // Given
        String nonExistentId = "NON-EXISTENT-ID";
        when(skuRepository.findById(SkuId.of(nonExistentId))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/inventory/skus/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound());
    }

}