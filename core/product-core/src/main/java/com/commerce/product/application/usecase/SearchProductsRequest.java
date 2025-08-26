package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.ProductStatus;
import java.util.Set;

/**
 * 상품 검색 요청
 */
public class SearchProductsRequest {
    private final String categoryId;
    private final String keyword;
    private final Integer minPrice;
    private final Integer maxPrice;
    private final Set<ProductStatus> statuses;
    private final int page;
    private final int size;
    private final String sortBy;
    private final String sortDirection;
    
    private SearchProductsRequest(Builder builder) {
        this.categoryId = builder.categoryId;
        this.keyword = builder.keyword;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.statuses = builder.statuses;
        this.page = builder.page;
        this.size = builder.size;
        this.sortBy = builder.sortBy;
        this.sortDirection = builder.sortDirection;
    }
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public Integer getMinPrice() {
        return minPrice;
    }
    
    public Integer getMaxPrice() {
        return maxPrice;
    }
    
    public Set<ProductStatus> getStatuses() {
        return statuses;
    }
    
    public int getPage() {
        return page;
    }
    
    public int getSize() {
        return size;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public String getSortDirection() {
        return sortDirection;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private static final Set<ProductStatus> DEFAULT_STATUSES = Set.of(ProductStatus.ACTIVE);
        private static final int DEFAULT_PAGE = 0;
        private static final int DEFAULT_SIZE = 20;
        private static final String DEFAULT_SORT_BY = "createdAt";
        private static final String DEFAULT_SORT_DIRECTION = "DESC";
        
        private String categoryId;
        private String keyword;
        private Integer minPrice;
        private Integer maxPrice;
        private Set<ProductStatus> statuses = DEFAULT_STATUSES;
        private int page = DEFAULT_PAGE;
        private int size = DEFAULT_SIZE;
        private String sortBy = DEFAULT_SORT_BY;
        private String sortDirection = DEFAULT_SORT_DIRECTION;
        
        public Builder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }
        
        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }
        
        public Builder minPrice(Integer minPrice) {
            this.minPrice = minPrice;
            return this;
        }
        
        public Builder maxPrice(Integer maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }
        
        public Builder statuses(Set<ProductStatus> statuses) {
            this.statuses = statuses;
            return this;
        }
        
        public Builder page(int page) {
            this.page = page;
            return this;
        }
        
        public Builder size(int size) {
            this.size = size;
            return this;
        }
        
        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }
        
        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }
        
        public SearchProductsRequest build() {
            return new SearchProductsRequest(this);
        }
    }
}