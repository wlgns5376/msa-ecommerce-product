package com.commerce.product.application.usecase;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 검색 응답
 */
public class SearchProductsResponse {
    private final List<SearchProductItem> products;
    private final PageInfo pageInfo;
    
    public SearchProductsResponse(List<SearchProductItem> products, PageInfo pageInfo) {
        this.products = products;
        this.pageInfo = pageInfo;
    }
    
    public List<SearchProductItem> getProducts() {
        return products;
    }
    
    public PageInfo getPageInfo() {
        return pageInfo;
    }
    
    public static class SearchProductItem {
        private final String productId;
        private final String name;
        private final String description;
        private final String productType;
        private final String status;
        private final BigDecimal minPrice;
        private final BigDecimal maxPrice;
        private final boolean isAvailable;
        private final List<String> categoryIds;
        
        private SearchProductItem(Builder builder) {
            this.productId = builder.productId;
            this.name = builder.name;
            this.description = builder.description;
            this.productType = builder.productType;
            this.status = builder.status;
            this.minPrice = builder.minPrice;
            this.maxPrice = builder.maxPrice;
            this.isAvailable = builder.isAvailable;
            this.categoryIds = builder.categoryIds;
        }
        
        public String getProductId() {
            return productId;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getProductType() {
            return productType;
        }
        
        public String getStatus() {
            return status;
        }
        
        public BigDecimal getMinPrice() {
            return minPrice;
        }
        
        public BigDecimal getMaxPrice() {
            return maxPrice;
        }
        
        public boolean isAvailable() {
            return isAvailable;
        }
        
        public List<String> getCategoryIds() {
            return categoryIds;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String productId;
            private String name;
            private String description;
            private String productType;
            private String status;
            private BigDecimal minPrice;
            private BigDecimal maxPrice;
            private boolean isAvailable;
            private List<String> categoryIds;
            
            public Builder productId(String productId) {
                this.productId = productId;
                return this;
            }
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            public Builder productType(String productType) {
                this.productType = productType;
                return this;
            }
            
            public Builder status(String status) {
                this.status = status;
                return this;
            }
            
            public Builder minPrice(BigDecimal minPrice) {
                this.minPrice = minPrice;
                return this;
            }
            
            public Builder maxPrice(BigDecimal maxPrice) {
                this.maxPrice = maxPrice;
                return this;
            }
            
            public Builder isAvailable(boolean isAvailable) {
                this.isAvailable = isAvailable;
                return this;
            }
            
            public Builder categoryIds(List<String> categoryIds) {
                this.categoryIds = categoryIds;
                return this;
            }
            
            public SearchProductItem build() {
                return new SearchProductItem(this);
            }
        }
    }
    
    public static class PageInfo {
        private final int currentPage;
        private final int pageSize;
        private final long totalElements;
        private final int totalPages;
        private final boolean hasNext;
        
        public PageInfo(int currentPage, int pageSize, long totalElements) {
            if (pageSize <= 0) {
                throw new IllegalArgumentException("Page size must be greater than zero.");
            }
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
            this.hasNext = currentPage < totalPages - 1;
        }
        
        public int getCurrentPage() {
            return currentPage;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        public long getTotalElements() {
            return totalElements;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public boolean hasNext() {
            return hasNext;
        }
    }
}