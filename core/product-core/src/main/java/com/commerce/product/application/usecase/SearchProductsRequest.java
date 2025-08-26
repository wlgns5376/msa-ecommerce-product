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
        private String categoryId;
        private String keyword;
        private Integer minPrice;
        private Integer maxPrice;
        private Set<ProductStatus> statuses = Set.of(ProductStatus.ACTIVE);
        private int page = 0;
        private int size = 20;
        private String sortBy = "createdAt";
        private String sortDirection = "DESC";
        
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