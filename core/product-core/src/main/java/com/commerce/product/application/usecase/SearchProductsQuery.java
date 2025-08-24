package com.commerce.product.application.usecase;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchProductsQuery {
    private final String keyword;
    private final int page;
    private final int size;
    @Builder.Default
    private final boolean onlyActive = false;
    
    public void validate() {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 키워드는 필수입니다");
        }
        
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다");
        }
        
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1에서 100 사이여야 합니다");
        }
    }
}