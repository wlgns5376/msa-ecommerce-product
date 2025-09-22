package com.commerce.inventory.api.controller;

import com.commerce.inventory.api.dto.ReceiveStockRequest;
import com.commerce.inventory.api.mapper.InventoryMapper;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import com.commerce.inventory.application.usecase.GetSkuByIdUseCase;
import com.commerce.inventory.application.usecase.ReceiveStockCommand;
import com.commerce.inventory.application.usecase.ReceiveStockUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {InventoryController.class, InventoryMapper.class})
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {

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
    @DisplayName("POST /api/inventory/skus/{id}/receive - 재고 입고 성공")
    void receiveStock_Success() throws Exception {
        // Given
        String skuId = "SKU001";
        ReceiveStockRequest request = ReceiveStockRequest.builder()
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
}