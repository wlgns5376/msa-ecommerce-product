package com.commerce.product.api.adapter.in.web;

import com.commerce.product.api.adapter.in.web.dto.AddProductOptionRequest;
import com.commerce.product.api.adapter.in.web.dto.AddProductOptionResponse;
import com.commerce.product.api.adapter.in.web.dto.CreateProductRequest;
import com.commerce.product.api.adapter.in.web.dto.ProductResponse;
import com.commerce.product.api.adapter.in.web.dto.UpdateProductRequest;
import com.commerce.product.api.adapter.in.web.dto.UpdateProductResponse;
import com.commerce.product.api.mapper.ProductMapper;
import com.commerce.product.application.usecase.AddProductOptionUseCase;
import com.commerce.product.application.usecase.CreateProductResponse;
import com.commerce.product.application.usecase.CreateProductUseCase;
import com.commerce.product.application.usecase.GetProductRequest;
import com.commerce.product.application.usecase.GetProductResponse;
import com.commerce.product.application.usecase.GetProductUseCase;
import com.commerce.product.application.usecase.UpdateProductUseCase;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.exception.ProductConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product", description = "상품 관리 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final AddProductOptionUseCase addProductOptionUseCase;
    private final ProductMapper productMapper;
    
    @Operation(summary = "상품 생성", description = "새로운 상품을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "상품 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        
        com.commerce.product.application.usecase.CreateProductRequest useCaseRequest = productMapper.toCreateProductRequest(request);
        CreateProductResponse useCaseResponse = createProductUseCase.createProduct(useCaseRequest);
        ProductResponse response = productMapper.toProductResponse(useCaseResponse);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "상품 조회", description = "상품 ID로 상품 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 조회 성공"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String id) {
        GetProductRequest request = new GetProductRequest(id);
        GetProductResponse useCaseResponse = getProductUseCase.execute(request);
        
        if (useCaseResponse == null) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        
        ProductResponse response = productMapper.toProductResponse(useCaseResponse);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "상품 수정", description = "기존 상품 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상품 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "동시 수정 충돌"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UpdateProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request) {
        
        com.commerce.product.application.usecase.UpdateProductRequest useCaseRequest = productMapper.toUpdateProductRequest(request, id);
        com.commerce.product.application.usecase.UpdateProductResponse useCaseResponse = updateProductUseCase.updateProduct(useCaseRequest);
        UpdateProductResponse response = productMapper.toUpdateProductResponse(useCaseResponse);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "상품 옵션 추가", description = "상품에 새로운 옵션을 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "옵션 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/{id}/options")
    public ResponseEntity<AddProductOptionResponse> addProductOption(
            @PathVariable String id,
            @Valid @RequestBody AddProductOptionRequest request) {
        
        com.commerce.product.application.usecase.AddProductOptionRequest useCaseRequest = productMapper.toAddProductOptionRequest(request, id);
        com.commerce.product.application.usecase.AddProductOptionResponse useCaseResponse = addProductOptionUseCase.addProductOption(useCaseRequest);
        AddProductOptionResponse response = productMapper.toAddProductOptionResponse(useCaseResponse);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
    
    @ExceptionHandler(InvalidProductException.class)
    public ResponseEntity<String> handleInvalidProductException(InvalidProductException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
    
    @ExceptionHandler(ProductConflictException.class)
    public ResponseEntity<String> handleProductConflictException(ProductConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}