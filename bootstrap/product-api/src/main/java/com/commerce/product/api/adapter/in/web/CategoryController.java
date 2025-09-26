package com.commerce.product.api.adapter.in.web;

import com.commerce.product.api.adapter.in.web.dto.CategoryResponse;
import com.commerce.product.api.adapter.in.web.dto.CreateCategoryApiRequest;
import com.commerce.product.api.mapper.CategoryMapper;
import com.commerce.product.application.usecase.CreateCategoryRequest;
import com.commerce.product.application.usecase.CreateCategoryResponse;
import com.commerce.product.application.usecase.CreateCategoryUseCase;
import com.commerce.product.domain.exception.InvalidCategoryHierarchyException;
import com.commerce.product.domain.exception.InvalidCategoryLevelException;
import com.commerce.product.domain.exception.InvalidCategoryNameException;
import com.commerce.product.domain.exception.MaxCategoryDepthException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Category", description = "카테고리 관리 API")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CreateCategoryUseCase createCategoryUseCase;
    private final CategoryMapper categoryMapper;
    
    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "카테고리 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 카테고리 이름, 레벨, 계층 구조)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryApiRequest request) {
        
        CreateCategoryRequest useCaseRequest = categoryMapper.toCreateCategoryRequest(request);
        CreateCategoryResponse useCaseResponse = createCategoryUseCase.execute(useCaseRequest);
        CategoryResponse response = categoryMapper.toCategoryResponse(useCaseResponse);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @ExceptionHandler(InvalidCategoryNameException.class)
    public ResponseEntity<String> handleInvalidCategoryNameException(InvalidCategoryNameException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
    
    @ExceptionHandler(InvalidCategoryLevelException.class)
    public ResponseEntity<String> handleInvalidCategoryLevelException(InvalidCategoryLevelException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
    
    @ExceptionHandler(InvalidCategoryHierarchyException.class)
    public ResponseEntity<String> handleInvalidCategoryHierarchyException(InvalidCategoryHierarchyException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
    
    @ExceptionHandler(MaxCategoryDepthException.class)
    public ResponseEntity<String> handleMaxCategoryDepthException(MaxCategoryDepthException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}