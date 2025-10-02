package com.commerce.product.api.mapper;

import com.commerce.product.api.adapter.in.web.dto.AddProductOptionRequest;
import com.commerce.product.api.adapter.in.web.dto.AddProductOptionResponse;
import com.commerce.product.api.adapter.in.web.dto.CreateProductRequest;
import com.commerce.product.api.adapter.in.web.dto.PaginatedProductsResponse;
import com.commerce.product.api.adapter.in.web.dto.ProductResponse;
import com.commerce.product.api.adapter.in.web.dto.UpdateProductRequest;
import com.commerce.product.api.adapter.in.web.dto.UpdateProductResponse;
import com.commerce.product.application.usecase.CreateProductResponse;
import com.commerce.product.application.usecase.GetProductResponse;
import com.commerce.product.application.usecase.GetProductsResponse;
import org.springframework.stereotype.Component;

/**
 * 상품 관련 DTO와 도메인 객체 간의 변환을 담당하는 매퍼
 * Controller와 UseCase 간의 데이터 변환 책임을 분리
 */
@Component
public class ProductMapper {
    
    /**
     * CreateProductRequest를 UseCase Request로 변환
     *
     * @param request 상품 생성 요청 DTO
     * @return UseCase 요청 객체
     */
    public com.commerce.product.application.usecase.CreateProductRequest toCreateProductRequest(CreateProductRequest request) {
        if (request == null) {
            return null;
        }
        
        return request.toUseCaseRequest();
    }
    
    /**
     * CreateProductResponse를 ProductResponse로 변환
     *
     * @param response UseCase 응답
     * @return API 응답 DTO
     */
    public ProductResponse toProductResponse(CreateProductResponse response) {
        if (response == null) {
            return null;
        }
        
        return ProductResponse.from(response);
    }
    
    /**
     * GetProductResponse를 ProductResponse로 변환
     *
     * @param response UseCase 응답
     * @return API 응답 DTO
     */
    public ProductResponse toProductResponse(GetProductResponse response) {
        if (response == null) {
            return null;
        }
        
        return ProductResponse.from(response);
    }
    
    /**
     * UpdateProductRequest를 UseCase Request로 변환
     *
     * @param request 상품 수정 요청 DTO
     * @param productId 상품 ID
     * @return UseCase 요청 객체
     */
    public com.commerce.product.application.usecase.UpdateProductRequest toUpdateProductRequest(UpdateProductRequest request, String productId) {
        if (request == null) {
            return null;
        }
        
        return request.toUseCaseRequest(productId);
    }
    
    /**
     * UpdateProductResponse(UseCase)를 UpdateProductResponse(DTO)로 변환
     *
     * @param response UseCase 응답
     * @return API 응답 DTO
     */
    public UpdateProductResponse toUpdateProductResponse(com.commerce.product.application.usecase.UpdateProductResponse response) {
        if (response == null) {
            return null;
        }
        
        return UpdateProductResponse.from(response);
    }
    
    /**
     * AddProductOptionRequest를 UseCase Request로 변환
     *
     * @param request 상품 옵션 추가 요청 DTO
     * @param productId 상품 ID
     * @return UseCase 요청 객체
     */
    public com.commerce.product.application.usecase.AddProductOptionRequest toAddProductOptionRequest(AddProductOptionRequest request, String productId) {
        if (request == null) {
            return null;
        }
        
        return request.toUseCaseRequest(productId);
    }
    
    /**
     * AddProductOptionResponse(UseCase)를 AddProductOptionResponse(DTO)로 변환
     *
     * @param response UseCase 응답
     * @return API 응답 DTO
     */
    public AddProductOptionResponse toAddProductOptionResponse(com.commerce.product.application.usecase.AddProductOptionResponse response) {
        if (response == null) {
            return null;
        }
        
        return AddProductOptionResponse.from(response);
    }
    
    /**
     * GetProductsResponse를 PaginatedProductsResponse로 변환
     *
     * @param response UseCase 응답
     * @return API 응답 DTO
     */
    public PaginatedProductsResponse toPaginatedProductsResponse(GetProductsResponse response) {
        if (response == null) {
            return null;
        }
        
        return PaginatedProductsResponse.from(response);
    }
}