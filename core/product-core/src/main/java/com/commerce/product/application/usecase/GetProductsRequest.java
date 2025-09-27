package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.model.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetProductsRequest {
    
    private String search;
    private ProductType type;
    private ProductStatus status;
    private Integer page;
    private Integer size;
    private String sort;
    
    public Integer getPage() {
        return page != null ? page : 0;
    }
    
    public Integer getSize() {
        return size != null && size > 0 ? size : 10;
    }
}