package com.commerce.product.api.adapter.in.web;

import com.commerce.product.api.adapter.in.web.dto.CreateCategoryApiRequest;
import com.commerce.product.api.adapter.in.web.dto.CategoryResponse;
import com.commerce.product.api.mapper.CategoryMapper;
import com.commerce.product.application.usecase.CreateCategoryUseCase;
import com.commerce.product.application.usecase.CreateCategoryRequest;
import com.commerce.product.application.usecase.CreateCategoryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;
    
    @Mock
    private CreateCategoryUseCase createCategoryUseCase;
    
    @Mock
    private CategoryMapper categoryMapper;
    
    @InjectMocks
    private CategoryController categoryController;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void createCategory_ShouldReturnCreatedCategory() throws Exception {
        // Given
        CreateCategoryApiRequest apiRequest = CreateCategoryApiRequest.builder()
                .name("Electronics")
                .parentId(null)
                .sortOrder(1)
                .build();
                
        CreateCategoryRequest useCaseRequest = CreateCategoryRequest.builder()
                .name("Electronics")
                .parentId(null)
                .sortOrder(1)
                .build();
                
        CreateCategoryResponse useCaseResponse = CreateCategoryResponse.builder()
                .categoryId("cat-001")
                .name("Electronics")
                .parentId(null)
                .level(1)
                .sortOrder(1)
                .isActive(true)
                .fullPath("/Electronics")
                .build();
                
        CategoryResponse apiResponse = CategoryResponse.builder()
                .categoryId("cat-001")
                .name("Electronics")
                .parentId(null)
                .level(1)
                .sortOrder(1)
                .active(true)
                .fullPath("/Electronics")
                .build();
                
        given(categoryMapper.toCreateCategoryRequest(any(CreateCategoryApiRequest.class)))
                .willReturn(useCaseRequest);
        given(createCategoryUseCase.execute(any(CreateCategoryRequest.class)))
                .willReturn(useCaseResponse);
        given(categoryMapper.toCategoryResponse(any(CreateCategoryResponse.class)))
                .willReturn(apiResponse);
                
        // When & Then
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categoryId").value("cat-001"))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.fullPath").value("/Electronics"));
    }
    
    @Test
    void createCategory_WithParent_ShouldReturnCreatedSubCategory() throws Exception {
        // Given
        CreateCategoryApiRequest apiRequest = CreateCategoryApiRequest.builder()
                .name("Smartphones")
                .parentId("cat-001")
                .sortOrder(1)
                .build();
                
        CreateCategoryRequest useCaseRequest = CreateCategoryRequest.builder()
                .name("Smartphones")
                .parentId("cat-001")
                .sortOrder(1)
                .build();
                
        CreateCategoryResponse useCaseResponse = CreateCategoryResponse.builder()
                .categoryId("cat-002")
                .name("Smartphones")
                .parentId("cat-001")
                .level(2)
                .sortOrder(1)
                .isActive(true)
                .fullPath("/Electronics/Smartphones")
                .build();
                
        CategoryResponse apiResponse = CategoryResponse.builder()
                .categoryId("cat-002")
                .name("Smartphones")
                .parentId("cat-001")
                .level(2)
                .sortOrder(1)
                .active(true)
                .fullPath("/Electronics/Smartphones")
                .build();
                
        given(categoryMapper.toCreateCategoryRequest(any(CreateCategoryApiRequest.class)))
                .willReturn(useCaseRequest);
        given(createCategoryUseCase.execute(any(CreateCategoryRequest.class)))
                .willReturn(useCaseResponse);
        given(categoryMapper.toCategoryResponse(any(CreateCategoryResponse.class)))
                .willReturn(apiResponse);
                
        // When & Then
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categoryId").value("cat-002"))
                .andExpect(jsonPath("$.name").value("Smartphones"))
                .andExpect(jsonPath("$.parentId").value("cat-001"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.fullPath").value("/Electronics/Smartphones"));
    }
    
    @Test
    void createCategory_WithInvalidName_ShouldReturnBadRequest() throws Exception {
        // Given
        CreateCategoryApiRequest apiRequest = CreateCategoryApiRequest.builder()
                .name("")
                .parentId(null)
                .sortOrder(1)
                .build();
                
        // When & Then
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void createCategory_WithNegativeSortOrder_ShouldReturnBadRequest() throws Exception {
        // Given
        CreateCategoryApiRequest apiRequest = CreateCategoryApiRequest.builder()
                .name("Electronics")
                .parentId(null)
                .sortOrder(-1)
                .build();
                
        // When & Then
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiRequest)))
                .andExpect(status().isBadRequest());
    }
}