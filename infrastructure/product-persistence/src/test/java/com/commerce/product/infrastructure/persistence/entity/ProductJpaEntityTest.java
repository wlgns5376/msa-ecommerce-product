package com.commerce.product.infrastructure.persistence.entity;

import com.commerce.product.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductJpaEntityTest {
    
    @Test
    @DisplayName("도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void fromDomainModel() {
        // Given
        ProductName productName = new ProductName("맥북 프로 16인치");
        String description = "Apple M3 Max 칩셋 탑재";
        Product product = Product.create(productName, description, ProductType.NORMAL);
        
        // When
        ProductJpaEntity entity = ProductJpaEntity.fromDomainModel(product);
        
        // Then
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getName()).isEqualTo(productName.value());
        assertThat(entity.getDescription()).isEqualTo(description);
        assertThat(entity.getType()).isEqualTo(ProductType.NORMAL);
        assertThat(entity.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(entity.isOutOfStock()).isFalse();
        assertThat(entity.getVersion()).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("JPA 엔티티를 도메인 모델로 변환할 수 있다")
    void toDomainModel() {
        // Given
        String productId = UUID.randomUUID().toString();
        ProductJpaEntity entity = ProductJpaEntity.builder()
                .id(productId)
                .name("맥북 프로 16인치")
                .description("Apple M3 Max 칩셋 탑재")
                .type(ProductType.NORMAL)
                .status(ProductStatus.ACTIVE)
                .outOfStock(false)
                .version(0L)
                .build();
        
        // When
        Product product = entity.toDomainModel();
        
        // Then
        assertThat(product.getId().value()).isEqualTo(productId);
        assertThat(product.getName().value()).isEqualTo("맥북 프로 16인치");
        assertThat(product.getDescription()).isEqualTo("Apple M3 Max 칩셋 탑재");
        assertThat(product.getType()).isEqualTo(ProductType.NORMAL);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.isOutOfStock()).isFalse();
        assertThat(product.getVersion()).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("옵션이 포함된 상품을 변환할 수 있다")
    void convertProductWithOptions() {
        // Given
        Product product = Product.create(
                new ProductName("맥북 프로"),
                "최신 맥북",
                ProductType.NORMAL
        );
        
        // 옵션 추가
        ProductOption option = ProductOption.single(
                "기본 모델",
                Money.of(3000000, Currency.KRW),
                "SKU001"
        );
        product.addOption(option);
        
        // When
        ProductJpaEntity entity = ProductJpaEntity.fromDomainModel(product);
        Product restoredProduct = entity.toDomainModel();
        
        // Then
        assertThat(entity.getOptions()).hasSize(1);
        assertThat(restoredProduct.getOptions()).hasSize(1);
        
        ProductOption restoredOption = restoredProduct.getOptions().get(0);
        assertThat(restoredOption.getName()).isEqualTo("기본 모델");
        assertThat(restoredOption.getPrice().amount()).isEqualTo(new java.math.BigDecimal("3000000"));
        assertThat(restoredOption.getSkuMapping().getSingleSkuId()).isEqualTo("SKU001");
    }
    
    @Test
    @DisplayName("카테고리가 연결된 상품을 변환할 수 있다")
    void convertProductWithCategories() {
        // Given
        Product product = Product.create(
                new ProductName("아이패드 프로"),
                "M3 칩셋 탑재",
                ProductType.NORMAL
        );
        
        // 카테고리 연결
        CategoryId categoryId1 = new CategoryId(UUID.randomUUID().toString());
        CategoryId categoryId2 = new CategoryId(UUID.randomUUID().toString());
        product.assignCategories(Arrays.asList(categoryId1, categoryId2));
        
        // When
        ProductJpaEntity entity = ProductJpaEntity.fromDomainModel(product);
        Product restoredProduct = entity.toDomainModel();
        
        // Then
        assertThat(entity.getCategories()).hasSize(2);
        assertThat(restoredProduct.getCategoryIds()).hasSize(2);
        assertThat(restoredProduct.getCategoryIds()).containsExactlyInAnyOrder(categoryId1, categoryId2);
    }
    
    @Test
    @DisplayName("번들 상품을 변환할 수 있다")
    void convertBundleProduct() {
        // Given
        Product product = Product.create(
                new ProductName("애플 번들 세트"),
                "맥북 + 아이패드 세트",
                ProductType.BUNDLE
        );
        
        // 번들 옵션 추가
        Map<String, Integer> bundleMappings = new HashMap<>();
        bundleMappings.put("MACBOOK_SKU", 1);
        bundleMappings.put("IPAD_SKU", 1);
        
        ProductOption bundleOption = ProductOption.bundle(
                "기본 세트",
                Money.of(5000000, Currency.KRW),
                SkuMapping.bundle(bundleMappings)
        );
        product.addOption(bundleOption);
        
        // When
        ProductJpaEntity entity = ProductJpaEntity.fromDomainModel(product);
        Product restoredProduct = entity.toDomainModel();
        
        // Then
        assertThat(restoredProduct.getType()).isEqualTo(ProductType.BUNDLE);
        assertThat(restoredProduct.getOptions()).hasSize(1);
        
        ProductOption restoredOption = restoredProduct.getOptions().get(0);
        assertThat(restoredOption.getSkuMapping().isBundle()).isTrue();
        assertThat(restoredOption.getSkuMapping().mappings()).hasSize(2);
    }
    
    @Test
    @DisplayName("재고 없음 상태의 상품을 변환할 수 있다")
    void convertOutOfStockProduct() {
        // Given
        Product product = Product.create(
                new ProductName("품절 상품"),
                "재고 없음",
                ProductType.NORMAL
        );
        product.markAsOutOfStock();
        
        // When
        ProductJpaEntity entity = ProductJpaEntity.fromDomainModel(product);
        Product restoredProduct = entity.toDomainModel();
        
        // Then
        assertThat(entity.isOutOfStock()).isTrue();
        assertThat(restoredProduct.isOutOfStock()).isTrue();
    }
}