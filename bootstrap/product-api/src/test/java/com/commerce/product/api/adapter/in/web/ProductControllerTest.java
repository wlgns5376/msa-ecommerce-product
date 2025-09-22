package com.commerce.product.api.adapter.in.web;

import com.commerce.product.api.adapter.in.web.dto.CreateProductRequest;
import com.commerce.product.api.adapter.in.web.dto.ProductResponse;
import com.commerce.product.api.adapter.in.web.dto.UpdateProductRequest;
import com.commerce.product.api.mapper.ProductMapper;
import com.commerce.product.application.usecase.CreateProductResponse;
import com.commerce.product.application.usecase.CreateProductUseCase;
import com.commerce.product.application.usecase.GetProductRequest;
import com.commerce.product.application.usecase.GetProductResponse;
import com.commerce.product.application.usecase.GetProductUseCase;
import com.commerce.product.application.usecase.UpdateProductResponse;
import com.commerce.product.application.usecase.UpdateProductUseCase;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.exception.ProductConflictException;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @MockBean
    private GetProductUseCase getProductUseCase;
    
    @MockBean
    private UpdateProductUseCase updateProductUseCase;
    
    @MockBean
    private ProductMapper productMapper;

    @Nested
    @DisplayName("POST /api/products - 상품 생성")
    class CreateProduct {

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
        @DisplayName("상품 생성 성공")
        void createProduct_Success() throws Exception {
            // Given
            when(productMapper.toCreateProductRequest(any())).thenReturn(createProductRequest.toUseCaseRequest());
            when(createProductUseCase.createProduct(any())).thenReturn(createProductResponse);
            when(productMapper.toProductResponse(any(CreateProductResponse.class))).thenReturn(ProductResponse.from(createProductResponse));

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
        @DisplayName("상품명 없이 요청 시 400 에러")
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
        @DisplayName("상품 타입 없이 요청 시 400 에러")
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
        @DisplayName("묶음 상품 생성 성공")
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

            when(productMapper.toCreateProductRequest(any())).thenReturn(bundleRequest.toUseCaseRequest());
            when(createProductUseCase.createProduct(any())).thenReturn(bundleResponse);
            when(productMapper.toProductResponse(any(CreateProductResponse.class))).thenReturn(ProductResponse.from(bundleResponse));

            // When & Then
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bundleRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value("PROD-002"))
                    .andExpect(jsonPath("$.type").value("BUNDLE"));
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id} - 상품 조회")
    class GetProduct {

        private GetProductResponse getProductResponse;

        @BeforeEach
        void setUp() {
            getProductResponse = GetProductResponse.builder()
                    .productId("PROD-001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .type(ProductType.NORMAL)
                    .status(ProductStatus.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("상품 ID로 조회 성공")
        void getProduct_Success() throws Exception {
            // Given
            String productId = "PROD-001";
            when(getProductUseCase.execute(any(GetProductRequest.class))).thenReturn(getProductResponse);
            when(productMapper.toProductResponse(any(GetProductResponse.class))).thenReturn(ProductResponse.from(getProductResponse));

            // When & Then
            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value("PROD-001"))
                    .andExpect(jsonPath("$.name").value("테스트 상품"))
                    .andExpect(jsonPath("$.description").value("테스트 상품 설명"))
                    .andExpect(jsonPath("$.type").value("NORMAL"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회 시 404 에러")
        void getProduct_NotFound() throws Exception {
            // Given
            String productId = "NON-EXISTENT";
            when(getProductUseCase.execute(any(GetProductRequest.class)))
                    .thenThrow(new IllegalArgumentException("Product not found: " + productId));

            // When & Then
            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("null 응답 시 404 에러")
        void getProduct_NullResponse() throws Exception {
            // Given
            String productId = "NULL-ID";
            when(getProductUseCase.execute(any(GetProductRequest.class)))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("묶음 상품 조회 성공")
        void getBundleProduct_Success() throws Exception {
            // Given
            String productId = "PROD-002";
            GetProductResponse bundleResponse = GetProductResponse.builder()
                    .productId("PROD-002")
                    .name("묶음 상품")
                    .description("묶음 상품 설명")
                    .type(ProductType.BUNDLE)
                    .status(ProductStatus.ACTIVE)
                    .build();
            
            when(getProductUseCase.execute(any(GetProductRequest.class))).thenReturn(bundleResponse);
            when(productMapper.toProductResponse(any(GetProductResponse.class))).thenReturn(ProductResponse.from(bundleResponse));

            // When & Then
            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value("PROD-002"))
                    .andExpect(jsonPath("$.type").value("BUNDLE"));
        }

        @Test
        @DisplayName("비활성 상품 조회 성공")
        void getInactiveProduct_Success() throws Exception {
            // Given
            String productId = "PROD-003";
            GetProductResponse inactiveResponse = GetProductResponse.builder()
                    .productId("PROD-003")
                    .name("비활성 상품")
                    .description("비활성 상품 설명")
                    .type(ProductType.NORMAL)
                    .status(ProductStatus.INACTIVE)
                    .build();
            
            when(getProductUseCase.execute(any(GetProductRequest.class))).thenReturn(inactiveResponse);
            when(productMapper.toProductResponse(any(GetProductResponse.class))).thenReturn(ProductResponse.from(inactiveResponse));

            // When & Then
            mockMvc.perform(get("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value("PROD-003"))
                    .andExpect(jsonPath("$.status").value("INACTIVE"));
        }
    }
    
    @Nested
    @DisplayName("PUT /api/products/{id} - 상품 수정")
    class UpdateProduct {
        
        private UpdateProductRequest updateProductRequest;
        private UpdateProductResponse updateProductResponse;
        private String productId;
        
        @BeforeEach
        void setUp() {
            productId = "PROD-001";
            
            updateProductRequest = UpdateProductRequest.builder()
                    .name("수정된 상품명")
                    .description("수정된 상품 설명")
                    .version(1L)
                    .build();
                    
            updateProductResponse = UpdateProductResponse.builder()
                    .productId(productId)
                    .name("수정된 상품명")
                    .description("수정된 상품 설명")
                    .type("NORMAL")
                    .status("ACTIVE")
                    .version(2L)
                    .build();
        }
        
        @Test
        @DisplayName("상품 수정 성공")
        void updateProduct_Success() throws Exception {
            // Given
            when(productMapper.toUpdateProductRequest(any(), anyString())).thenReturn(updateProductRequest.toUseCaseRequest(productId));
            when(updateProductUseCase.updateProduct(any())).thenReturn(updateProductResponse);
            when(productMapper.toUpdateProductResponse(any())).thenReturn(com.commerce.product.api.adapter.in.web.dto.UpdateProductResponse.from(updateProductResponse));
            
            // When & Then
            mockMvc.perform(put("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateProductRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(productId))
                    .andExpect(jsonPath("$.name").value("수정된 상품명"))
                    .andExpect(jsonPath("$.description").value("수정된 상품 설명"))
                    .andExpect(jsonPath("$.type").value("NORMAL"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.version").value(2L));
        }
        
        @Test
        @DisplayName("상품명 없이 수정 시 400 에러")
        void updateProduct_WithoutName_ShouldReturn400() throws Exception {
            // Given
            UpdateProductRequest invalidRequest = UpdateProductRequest.builder()
                    .description("수정된 상품 설명")
                    .version(1L)
                    .build();
            
            // When & Then
            mockMvc.perform(put("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("버전 정보 없이 수정 시 400 에러")
        void updateProduct_WithoutVersion_ShouldReturn400() throws Exception {
            // Given
            UpdateProductRequest invalidRequest = UpdateProductRequest.builder()
                    .name("수정된 상품명")
                    .description("수정된 상품 설명")
                    .build();
            
            // When & Then
            mockMvc.perform(put("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("존재하지 않는 상품 수정 시 404 에러")
        void updateProduct_NotFound() throws Exception {
            // Given
            String nonExistentId = "NON-EXISTENT";
            when(updateProductUseCase.updateProduct(any()))
                    .thenThrow(new InvalidProductException("Product not found with id: " + nonExistentId));
            
            // When & Then
            mockMvc.perform(put("/api/products/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateProductRequest)))
                    .andExpect(status().isNotFound());
        }
        
        @Test
        @DisplayName("동시 수정 충돌 시 409 에러")
        void updateProduct_VersionConflict() throws Exception {
            // Given
            when(updateProductUseCase.updateProduct(any()))
                    .thenThrow(new ProductConflictException("Product has been modified by another user. Please refresh and try again."));
            
            // When & Then
            mockMvc.perform(put("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateProductRequest)))
                    .andExpect(status().isConflict());
        }
        
        @Test
        @DisplayName("설명만 수정 성공")
        void updateProduct_DescriptionOnly_Success() throws Exception {
            // Given
            UpdateProductRequest descriptionUpdateRequest = UpdateProductRequest.builder()
                    .name("기존 상품명")
                    .description("새로운 설명")
                    .version(1L)
                    .build();
                    
            UpdateProductResponse descriptionUpdateResponse = UpdateProductResponse.builder()
                    .productId(productId)
                    .name("기존 상품명")
                    .description("새로운 설명")
                    .type("NORMAL")
                    .status("ACTIVE")
                    .version(2L)
                    .build();
                    
            when(productMapper.toUpdateProductRequest(any(), anyString())).thenReturn(descriptionUpdateRequest.toUseCaseRequest(productId));
            when(updateProductUseCase.updateProduct(any())).thenReturn(descriptionUpdateResponse);
            when(productMapper.toUpdateProductResponse(any())).thenReturn(com.commerce.product.api.adapter.in.web.dto.UpdateProductResponse.from(descriptionUpdateResponse));
            
            // When & Then
            mockMvc.perform(put("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(descriptionUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("새로운 설명"))
                    .andExpect(jsonPath("$.version").value(2L));
        }
    }
}