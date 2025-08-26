package com.commerce.product.domain.repository;

import com.commerce.product.domain.model.ProductStatus;
import java.util.Set;

/**
 * 상품 검색 조건
 */
public class ProductSearchCriteria {
    private final String categoryId;
    private final String keyword;
    private final Integer minPrice;
    private final Integer maxPrice;
    private final Set<ProductStatus> statuses;
    
    private ProductSearchCriteria(Builder builder) {
        this.categoryId = builder.categoryId;
        this.keyword = builder.keyword;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.statuses = builder.statuses;
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
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String categoryId;
        private String keyword;
        private Integer minPrice;
        private Integer maxPrice;
        private Set<ProductStatus> statuses;
        
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
        
        public ProductSearchCriteria build() {
            return new ProductSearchCriteria(this);
        }
    }
}