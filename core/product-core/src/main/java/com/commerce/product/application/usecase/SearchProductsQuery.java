package com.commerce.product.application.usecase;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchProductsQuery {
    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 100;
    
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
        
        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(String.format("페이지 크기는 %d에서 %d 사이여야 합니다", MIN_PAGE_SIZE, MAX_PAGE_SIZE));
        }
    }
}