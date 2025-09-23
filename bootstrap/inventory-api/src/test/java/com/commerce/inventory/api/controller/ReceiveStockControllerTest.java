package com.commerce.inventory.api.controller;

import com.commerce.inventory.api.dto.ReceiveStockRequest;
import com.commerce.inventory.api.mapper.InventoryMapper;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import com.commerce.inventory.application.usecase.GetSkuByIdUseCase;
import com.commerce.inventory.application.usecase.ReceiveStockCommand;
import com.commerce.inventory.application.usecase.ReceiveStockUseCase;
import com.commerce.inventory.domain.exception.SkuNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@ContextConfiguration(classes = {InventoryController.class, com.commerce.inventory.api.exception.GlobalExceptionHandler.class})
class ReceiveStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReceiveStockUseCase receiveStockUseCase;

    @MockBean
    private CreateSkuUseCase createSkuUseCase;

    @MockBean
    private GetSkuByIdUseCase getSkuByIdUseCase;

    @MockBean
    private InventoryMapper inventoryMapper;

    @Test
    @DisplayName("재고 입고 - 정상적으로 재고를 입고한다")
    void receiveStock_Success() throws Exception {
        // Given
        String skuId = "SKU001";
        ReceiveStockRequest request = ReceiveStockRequest.builder()
                .quantity(100)
                .reference("PO-2024-001")
                .build();

        ReceiveStockCommand command = ReceiveStockCommand.builder()
                .skuId(skuId)
                .quantity(100)
                .reference("PO-2024-001")
                .build();

        doNothing().when(receiveStockUseCase).receive(any(ReceiveStockCommand.class));

        // When & Then
        mockMvc.perform(post("/api/inventory/skus/{id}/receive", skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(receiveStockUseCase, times(1)).receive(argThat(cmd ->
                cmd.getSkuId().equals(skuId) &&
                        cmd.getQuantity() == 100 &&
                        cmd.getReference().equals("PO-2024-001")
        ));
    }

    @Test
    @DisplayName("재고 입고 - 수량이 0이하인 경우 400 에러 반환")
    void receiveStock_InvalidQuantity_BadRequest() throws Exception {
        // Given
        String skuId = "SKU001";
        ReceiveStockRequest request = ReceiveStockRequest.builder()
                .quantity(0)
                .reference("PO-2024-001")
                .build();

        // When & Then
        mockMvc.perform(post("/api/inventory/skus/{id}/receive", skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 입고 - 음수 수량인 경우 400 에러 반환")
    void receiveStock_NegativeQuantity_BadRequest() throws Exception {
        // Given
        String skuId = "SKU001";
        ReceiveStockRequest request = ReceiveStockRequest.builder()
                .quantity(-10)
                .reference("PO-2024-001")
                .build();

        // When & Then
        mockMvc.perform(post("/api/inventory/skus/{id}/receive", skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 입고 - 참조 번호가 없는 경우 400 에러 반환")
    void receiveStock_MissingReference_BadRequest() throws Exception {
        // Given
        String skuId = "SKU001";
        ReceiveStockRequest request = ReceiveStockRequest.builder()
                .quantity(100)
                .reference(null)
                .build();

        // When & Then
        mockMvc.perform(post("/api/inventory/skus/{id}/receive", skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 입고 - 빈 참조 번호인 경우 400 에러 반환")
    void receiveStock_EmptyReference_BadRequest() throws Exception {
        // Given
        String skuId = "SKU001";
        ReceiveStockRequest request = ReceiveStockRequest.builder()
                .quantity(100)
                .reference("")
                .build();

        // When & Then
        mockMvc.perform(post("/api/inventory/skus/{id}/receive", skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("재고 입고 - 존재하지 않는 SKU인 경우 404 에러 반환")
    void receiveStock_SkuNotFound_NotFound() throws Exception {
        // Given
        String skuId = "NON_EXISTENT_SKU";
        ReceiveStockRequest request = ReceiveStockRequest.builder()
                .quantity(100)
                .reference("PO-2024-001")
                .build();

        doThrow(new SkuNotFoundException("SKU를 찾을 수 없습니다: " + skuId))
                .when(receiveStockUseCase).receive(any(ReceiveStockCommand.class));

        // When & Then
        mockMvc.perform(post("/api/inventory/skus/{id}/receive", skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("재고 입고 - 대량 입고 처리")
    void receiveStock_LargeQuantity_Success() throws Exception {
        // Given
        String skuId = "SKU001";
        ReceiveStockRequest request = ReceiveStockRequest.builder()
                .quantity(10000)
                .reference("PO-2024-BULK")
                .build();

        doNothing().when(receiveStockUseCase).receive(any(ReceiveStockCommand.class));

        // When & Then
        mockMvc.perform(post("/api/inventory/skus/{id}/receive", skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(receiveStockUseCase, times(1)).receive(argThat(cmd ->
                cmd.getQuantity() == 10000
        ));
    }
}