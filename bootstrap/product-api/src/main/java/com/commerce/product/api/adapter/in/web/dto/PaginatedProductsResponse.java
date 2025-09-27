package com.commerce.product.api.adapter.in.web.dto;

import com.commerce.product.application.usecase.GetProductsResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "상품 목록 페이지네이션 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedProductsResponse {
    
    @Schema(description = "상품 목록")
    private List<ProductResponse> products;
    
    @Schema(description = "전체 상품 수", example = "100")
    private Long totalElements;
    
    @Schema(description = "전체 페이지 수", example = "10")
    private Integer totalPages;
    
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private Integer pageNumber;
    
    @Schema(description = "페이지당 상품 수", example = "10")
    private Integer pageSize;
    
    public static PaginatedProductsResponse from(GetProductsResponse response) {
        return PaginatedProductsResponse.builder()
                .products(response.getProducts().stream()
                        .map(ProductResponse::from)
                        .collect(Collectors.toList()))
                .totalElements(response.getTotalElements())
                .totalPages(response.getTotalPages())
                .pageNumber(response.getPageNumber())
                .pageSize(response.getPageSize())
                .build();
    }
}