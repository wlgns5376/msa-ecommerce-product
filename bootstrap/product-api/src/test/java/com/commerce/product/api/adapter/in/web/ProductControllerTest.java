package com.commerce.product.api.adapter.in.web;

import com.commerce.product.api.adapter.in.web.dto.AddProductOptionRequest;
import com.commerce.product.api.adapter.in.web.dto.AddProductOptionResponse;
import com.commerce.product.api.adapter.in.web.dto.CreateProductRequest;
import com.commerce.product.api.adapter.in.web.dto.ProductResponse;
import com.commerce.product.api.adapter.in.web.dto.UpdateProductRequest;
import com.commerce.product.api.mapper.ProductMapper;
import com.commerce.product.application.usecase.AddProductOptionUseCase;
import com.commerce.product.application.usecase.CreateProductResponse;
import com.commerce.product.application.usecase.CreateProductUseCase;
import com.commerce.product.application.usecase.GetProductRequest;
import com.commerce.product.application.usecase.GetProductResponse;
import com.commerce.product.application.usecase.GetProductUseCase;
import com.commerce.product.application.usecase.UpdateProductResponse;
import com.commerce.product.application.usecase.UpdateProductUseCase;
import com.commerce.product.application.usecase.GetProductsUseCase;
import com.commerce.product.application.usecase.GetProductsRequest;
import com.commerce.product.application.usecase.GetProductsResponse;
import com.commerce.product.api.adapter.in.web.dto.PaginatedProductsResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private AddProductOptionUseCase addProductOptionUseCase;
    
    @MockBean
    private GetProductsUseCase getProductsUseCase;
    
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
    
    @Nested
    @DisplayName("POST /api/products/{id}/options - 상품 옵션 추가")
    class AddProductOption {
        
        private AddProductOptionRequest addProductOptionRequest;
        private com.commerce.product.application.usecase.AddProductOptionResponse addProductOptionResponse;
        private String productId;
        
        @BeforeEach
        void setUp() {
            productId = "PROD-001";
            
            Map<String, Integer> skuMappings = new HashMap<>(); // key: SKU ID, value: 수량
            skuMappings.put("SKU-001", 1); // SKU-001을 1개 포함
            skuMappings.put("SKU-002", 2); // SKU-002를 2개 포함
            
            addProductOptionRequest = AddProductOptionRequest.builder()
                    .optionName("기본 옵션")
                    .price(new BigDecimal("10000"))
                    .currency("KRW")
                    .skuMappings(skuMappings)
                    .build();
                    
            addProductOptionResponse = com.commerce.product.application.usecase.AddProductOptionResponse.builder()
                    .productId(productId)
                    .optionId("OPT-001")
                    .optionName("기본 옵션")
                    .build();
        }
        
        @Test
        @DisplayName("상품 옵션 추가 성공")
        void addProductOption_Success() throws Exception {
            // Given
            when(productMapper.toAddProductOptionRequest(any(), anyString())).thenReturn(addProductOptionRequest.toUseCaseRequest(productId));
            when(addProductOptionUseCase.addProductOption(any())).thenReturn(addProductOptionResponse);
            when(productMapper.toAddProductOptionResponse(any())).thenReturn(AddProductOptionResponse.from(addProductOptionResponse));
            
            // When & Then
            mockMvc.perform(post("/api/products/{id}/options", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addProductOptionRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(productId))
                    .andExpect(jsonPath("$.optionId").value("OPT-001"))
                    .andExpect(jsonPath("$.optionName").value("기본 옵션"));
        }
        
        @Test
        @DisplayName("옵션명 없이 추가 시 400 에러")
        void addProductOption_WithoutOptionName_ShouldReturn400() throws Exception {
            // Given
            AddProductOptionRequest invalidRequest = AddProductOptionRequest.builder()
                    .price(new BigDecimal("10000"))
                    .currency("KRW")
                    .skuMappings(new HashMap<>())
                    .build();
            
            // When & Then
            mockMvc.perform(post("/api/products/{id}/options", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("가격 없이 추가 시 400 에러")
        void addProductOption_WithoutPrice_ShouldReturn400() throws Exception {
            // Given
            AddProductOptionRequest invalidRequest = AddProductOptionRequest.builder()
                    .optionName("기본 옵션")
                    .currency("KRW")
                    .skuMappings(new HashMap<>())
                    .build();
            
            // When & Then
            mockMvc.perform(post("/api/products/{id}/options", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("SKU 매핑 없이 추가 시 400 에러")
        void addProductOption_WithoutSkuMappings_ShouldReturn400() throws Exception {
            // Given
            AddProductOptionRequest invalidRequest = AddProductOptionRequest.builder()
                    .optionName("기본 옵션")
                    .price(new BigDecimal("10000"))
                    .currency("KRW")
                    .build();
            
            // When & Then
            mockMvc.perform(post("/api/products/{id}/options", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("존재하지 않는 상품에 옵션 추가 시 404 에러")
        void addProductOption_ProductNotFound() throws Exception {
            // Given
            String nonExistentId = "NON-EXISTENT";
            when(addProductOptionUseCase.addProductOption(any()))
                    .thenThrow(new InvalidProductException("Product not found with id: " + nonExistentId));
            
            // When & Then
            mockMvc.perform(post("/api/products/{id}/options", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addProductOptionRequest)))
                    .andExpect(status().isNotFound());
        }
        
        @Test
        @DisplayName("묶음 상품에 옵션 추가 성공")
        void addProductOption_ToBundleProduct_Success() throws Exception {
            // Given
            String bundleProductId = "PROD-BUNDLE-001";
            
            Map<String, Integer> bundleSkuMappings = new HashMap<>(); // key: SKU ID, value: 수량
            bundleSkuMappings.put("SKU-001", 2); // SKU-001을 2개 포함
            bundleSkuMappings.put("SKU-002", 3); // SKU-002를 3개 포함
            bundleSkuMappings.put("SKU-003", 1); // SKU-003을 1개 포함
            
            AddProductOptionRequest bundleOptionRequest = AddProductOptionRequest.builder()
                    .optionName("묶음 옵션")
                    .price(new BigDecimal("25000"))
                    .currency("KRW")
                    .skuMappings(bundleSkuMappings)
                    .build();
                    
            com.commerce.product.application.usecase.AddProductOptionResponse bundleResponse = com.commerce.product.application.usecase.AddProductOptionResponse.builder()
                    .productId(bundleProductId)
                    .optionId("OPT-BUNDLE-001")
                    .optionName("묶음 옵션")
                    .build();
            
            when(productMapper.toAddProductOptionRequest(any(), anyString())).thenReturn(bundleOptionRequest.toUseCaseRequest(bundleProductId));
            when(addProductOptionUseCase.addProductOption(any())).thenReturn(bundleResponse);
            when(productMapper.toAddProductOptionResponse(any())).thenReturn(AddProductOptionResponse.from(bundleResponse));
            
            // When & Then
            mockMvc.perform(post("/api/products/{id}/options", bundleProductId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bundleOptionRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(bundleProductId))
                    .andExpect(jsonPath("$.optionId").value("OPT-BUNDLE-001"))
                    .andExpect(jsonPath("$.optionName").value("묶음 옵션"));
        }
    }
    
    @Nested
    @DisplayName("GET /api/products - 상품 목록 조회")
    class GetProducts {
        
        private GetProductsResponse getProductsResponse;
        private List<GetProductResponse> products;
        
        @BeforeEach
        void setUp() {
            products = Arrays.asList(
                GetProductResponse.builder()
                    .productId("PROD-001")
                    .name("상품1")
                    .description("상품1 설명")
                    .type(ProductType.NORMAL)
                    .status(ProductStatus.ACTIVE)
                    .build(),
                GetProductResponse.builder()
                    .productId("PROD-002")
                    .name("상품2")
                    .description("상품2 설명")
                    .type(ProductType.BUNDLE)
                    .status(ProductStatus.ACTIVE)
                    .build()
            );
            
            getProductsResponse = GetProductsResponse.builder()
                .products(products)
                .totalElements(2L)
                .totalPages(1)
                .pageNumber(0)
                .pageSize(10)
                .build();
        }
        
        @Test
        @DisplayName("전체 상품 목록 조회 성공")
        void getProducts_Success() throws Exception {
            // Given
            when(getProductsUseCase.execute(any(GetProductsRequest.class)))
                .thenReturn(getProductsResponse);
            when(productMapper.toPaginatedProductsResponse(any(GetProductsResponse.class)))
                .thenReturn(PaginatedProductsResponse.from(getProductsResponse));
            
            // When & Then
            mockMvc.perform(get("/api/products")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products.length()").value(2))
                .andExpect(jsonPath("$.products[0].productId").value("PROD-001"))
                .andExpect(jsonPath("$.products[0].name").value("상품1"))
                .andExpect(jsonPath("$.products[1].productId").value("PROD-002"))
                .andExpect(jsonPath("$.products[1].name").value("상품2"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
        }
        
        @Test
        @DisplayName("검색 키워드로 상품 목록 조회 성공")
        void getProducts_WithSearch_Success() throws Exception {
            // Given
            List<GetProductResponse> searchResults = Collections.singletonList(
                GetProductResponse.builder()
                    .productId("PROD-001")
                    .name("상품1")
                    .description("상품1 설명")
                    .type(ProductType.NORMAL)
                    .status(ProductStatus.ACTIVE)
                    .build()
            );
            
            GetProductsResponse searchResponse = GetProductsResponse.builder()
                .products(searchResults)
                .totalElements(1L)
                .totalPages(1)
                .pageNumber(0)
                .pageSize(10)
                .build();
            
            when(getProductsUseCase.execute(any(GetProductsRequest.class)))
                .thenReturn(searchResponse);
            when(productMapper.toPaginatedProductsResponse(any(GetProductsResponse.class)))
                .thenReturn(PaginatedProductsResponse.from(searchResponse));
            
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("search", "상품1")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.products[0].name").value("상품1"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }
        
        @Test
        @DisplayName("상품 타입으로 필터링 조회 성공")
        void getProducts_WithTypeFilter_Success() throws Exception {
            // Given
            List<GetProductResponse> bundleProducts = Collections.singletonList(
                GetProductResponse.builder()
                    .productId("PROD-002")
                    .name("상품2")
                    .description("상품2 설명")
                    .type(ProductType.BUNDLE)
                    .status(ProductStatus.ACTIVE)
                    .build()
            );
            
            GetProductsResponse filterResponse = GetProductsResponse.builder()
                .products(bundleProducts)
                .totalElements(1L)
                .totalPages(1)
                .pageNumber(0)
                .pageSize(10)
                .build();
            
            when(getProductsUseCase.execute(any(GetProductsRequest.class)))
                .thenReturn(filterResponse);
            when(productMapper.toPaginatedProductsResponse(any(GetProductsResponse.class)))
                .thenReturn(PaginatedProductsResponse.from(filterResponse));
            
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("type", "BUNDLE")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.products[0].type").value("BUNDLE"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }
        
        @Test
        @DisplayName("상품 상태로 필터링 조회 성공")
        void getProducts_WithStatusFilter_Success() throws Exception {
            // Given
            when(getProductsUseCase.execute(any(GetProductsRequest.class)))
                .thenReturn(getProductsResponse);
            when(productMapper.toPaginatedProductsResponse(any(GetProductsResponse.class)))
                .thenReturn(PaginatedProductsResponse.from(getProductsResponse));
            
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("status", "ACTIVE")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.products[1].status").value("ACTIVE"));
        }
        
        @Test
        @DisplayName("페이지네이션 조회 성공")
        void getProducts_WithPagination_Success() throws Exception {
            // Given
            GetProductsResponse paginatedResponse = GetProductsResponse.builder()
                .products(products)
                .totalElements(50L)
                .totalPages(5)
                .pageNumber(1)
                .pageSize(10)
                .build();
            
            when(getProductsUseCase.execute(any(GetProductsRequest.class)))
                .thenReturn(paginatedResponse);
            when(productMapper.toPaginatedProductsResponse(any(GetProductsResponse.class)))
                .thenReturn(PaginatedProductsResponse.from(paginatedResponse));
            
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("page", "1")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.totalElements").value(50));
        }
        
        @Test
        @DisplayName("정렬 조건으로 조회 성공")
        void getProducts_WithSort_Success() throws Exception {
            // Given
            when(getProductsUseCase.execute(any(GetProductsRequest.class)))
                .thenReturn(getProductsResponse);
            when(productMapper.toPaginatedProductsResponse(any(GetProductsResponse.class)))
                .thenReturn(PaginatedProductsResponse.from(getProductsResponse));
            
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("sort", "name,asc")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());
        }
        
        @Test
        @DisplayName("복합 조건 조회 성공")
        void getProducts_WithMultipleConditions_Success() throws Exception {
            // Given
            when(getProductsUseCase.execute(any(GetProductsRequest.class)))
                .thenReturn(getProductsResponse);
            when(productMapper.toPaginatedProductsResponse(any(GetProductsResponse.class)))
                .thenReturn(PaginatedProductsResponse.from(getProductsResponse));
            
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("search", "상품")
                    .param("type", "NORMAL")
                    .param("status", "ACTIVE")
                    .param("page", "0")
                    .param("size", "20")
                    .param("sort", "createdAt,desc")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());
        }
        
        @Test
        @DisplayName("빈 결과 조회 성공")
        void getProducts_EmptyResult_Success() throws Exception {
            // Given
            GetProductsResponse emptyResponse = GetProductsResponse.builder()
                .products(Collections.emptyList())
                .totalElements(0L)
                .totalPages(0)
                .pageNumber(0)
                .pageSize(10)
                .build();
            
            when(getProductsUseCase.execute(any(GetProductsRequest.class)))
                .thenReturn(emptyResponse);
            when(productMapper.toPaginatedProductsResponse(any(GetProductsResponse.class)))
                .thenReturn(PaginatedProductsResponse.from(emptyResponse));
            
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("search", "존재하지않는상품")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
        }
        
        @Test
        @DisplayName("잘못된 페이지 번호 요청 시 400 에러")
        void getProducts_InvalidPageNumber_ShouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("page", "-1")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("잘못된 페이지 크기 요청 시 400 에러")
        void getProducts_InvalidPageSize_ShouldReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/products")
                    .param("size", "0")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
    }
}