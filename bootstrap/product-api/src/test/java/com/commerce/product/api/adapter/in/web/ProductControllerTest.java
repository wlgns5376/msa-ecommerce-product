package com.commerce.product.api.adapter.in.web;

import com.commerce.product.api.adapter.in.web.dto.CreateProductRequest;
import com.commerce.product.api.adapter.in.web.dto.ProductResponse;
import com.commerce.product.application.usecase.CreateProductResponse;
import com.commerce.product.application.usecase.CreateProductUseCase;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateProductUseCase createProductUseCase;

    private CreateProductRequest createProductRequest;
    private CreateProductResponse createProductResponse;

    @BeforeEach
    void setUp() {
        createProductRequest = CreateProductRequest.builder()
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .type(ProductType.NORMAL)
                .build();

        createProductResponse = CreateProductResponse.builder()
                .productId("PROD-001")
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .type(ProductType.NORMAL)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("POST /api/products - 상품 생성 성공")
    void createProduct_Success() throws Exception {
        // Given
        when(createProductUseCase.createProduct(any())).thenReturn(createProductResponse);

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProductRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("PROD-001"))
                .andExpect(jsonPath("$.name").value("테스트 상품"))
                .andExpect(jsonPath("$.description").value("테스트 상품 설명"))
                .andExpect(jsonPath("$.type").value("NORMAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/products - 상품명 없이 요청 시 400 에러")
    void createProduct_WithoutName_ShouldReturn400() throws Exception {
        // Given
        CreateProductRequest invalidRequest = CreateProductRequest.builder()
                .description("테스트 상품 설명")
                .type(ProductType.NORMAL)
                .build();

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - 상품 타입 없이 요청 시 400 에러")
    void createProduct_WithoutType_ShouldReturn400() throws Exception {
        // Given
        String requestJson = "{\"name\":\"테스트 상품\",\"description\":\"테스트 상품 설명\"}";

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products - 묶음 상품 생성 성공")
    void createBundleProduct_Success() throws Exception {
        // Given
        CreateProductRequest bundleRequest = CreateProductRequest.builder()
                .name("묶음 상품")
                .description("묶음 상품 설명")
                .type(ProductType.BUNDLE)
                .build();

        CreateProductResponse bundleResponse = CreateProductResponse.builder()
                .productId("PROD-002")
                .name("묶음 상품")
                .description("묶음 상품 설명")
                .type(ProductType.BUNDLE)
                .status(ProductStatus.ACTIVE)
                .build();

        when(createProductUseCase.createProduct(any())).thenReturn(bundleResponse);

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("PROD-002"))
                .andExpect(jsonPath("$.type").value("BUNDLE"));
    }
}