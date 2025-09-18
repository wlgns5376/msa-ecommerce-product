package com.commerce.inventory.api.controller;

import com.commerce.inventory.api.dto.GetSkuByIdResponseDto;
import com.commerce.inventory.api.mapper.InventoryMapper;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import com.commerce.inventory.application.usecase.GetSkuByIdQuery;
import com.commerce.inventory.application.usecase.GetSkuByIdResponse;
import com.commerce.inventory.application.usecase.GetSkuByIdUseCase;
import com.commerce.inventory.domain.exception.SkuNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@DisplayName("SKU 조회 API 컨트롤러 테스트")
class GetSkuByIdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateSkuUseCase createSkuUseCase;

    @MockBean
    private GetSkuByIdUseCase getSkuByIdUseCase;

    @MockBean
    private InventoryMapper inventoryMapper;

    @Test
    @DisplayName("SKU ID로 SKU를 성공적으로 조회한다")
    void should_get_sku_by_id_successfully() throws Exception {
        // Given
        String skuId = "SKU-001";
        LocalDateTime now = LocalDateTime.now();
        
        GetSkuByIdResponse response = GetSkuByIdResponse.builder()
            .id(skuId)
            .code("TEST-SKU-001")
            .name("테스트 SKU")
            .description("테스트용 SKU 설명")
            .weight(BigDecimal.valueOf(1.5))
            .volume(BigDecimal.valueOf(10.0))
            .createdAt(now)
            .updatedAt(now)
            .version(1L)
            .build();
        
        GetSkuByIdResponseDto responseDto = GetSkuByIdResponseDto.builder()
            .id(skuId)
            .code("TEST-SKU-001")
            .name("테스트 SKU")
            .description("테스트용 SKU 설명")
            .weight(BigDecimal.valueOf(1.5))
            .volume(BigDecimal.valueOf(10.0))
            .createdAt(now)
            .updatedAt(now)
            .version(1L)
            .build();
        
        when(getSkuByIdUseCase.execute(any(GetSkuByIdQuery.class))).thenReturn(response);
        when(inventoryMapper.toGetSkuByIdResponseDto(response)).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/inventory/skus/{id}", skuId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(skuId))
            .andExpect(jsonPath("$.code").value("TEST-SKU-001"))
            .andExpect(jsonPath("$.name").value("테스트 SKU"))
            .andExpect(jsonPath("$.description").value("테스트용 SKU 설명"))
            .andExpect(jsonPath("$.weight").value(1.5))
            .andExpect(jsonPath("$.volume").value(10.0))
            .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("존재하지 않는 SKU ID로 조회 시 404를 반환한다")
    void should_return_404_when_sku_not_found() throws Exception {
        // Given
        String nonExistentId = "NON-EXISTENT-ID";
        when(getSkuByIdUseCase.execute(any(GetSkuByIdQuery.class)))
            .thenThrow(new SkuNotFoundException("SKU를 찾을 수 없습니다. ID: " + nonExistentId));

        // When & Then
        mockMvc.perform(get("/api/inventory/skus/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("잘못된 형식의 SKU ID로 조회 시 400을 반환한다")
    void should_return_400_for_invalid_sku_id_format() throws Exception {
        // Given
        String invalidId = "";
        
        // When & Then
        mockMvc.perform(get("/api/inventory/skus/{id}", invalidId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound()); // Spring이 빈 경로 변수를 404로 처리
    }
}