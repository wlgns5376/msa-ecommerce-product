package com.commerce.product.application.usecase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetProductsResponse {
    
    private List<GetProductResponse> products;
    private Long totalElements;
    private Integer totalPages;
    private Integer pageNumber;
    private Integer pageSize;
}