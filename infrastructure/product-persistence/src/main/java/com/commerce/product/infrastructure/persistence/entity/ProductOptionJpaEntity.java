package com.commerce.product.infrastructure.persistence.entity;

import com.commerce.product.domain.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductOptionJpaEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "price_amount", nullable = false)
    private BigDecimal priceAmount;
    
    @Column(name = "price_currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency priceCurrency;
    
    @Column(name = "is_bundle", nullable = false)
    @Builder.Default
    private boolean bundle = false;
    
    @Column(name = "sku_mapping", columnDefinition = "TEXT")
    private String skuMapping;  // JSON 형태로 저장
    
    public static ProductOptionJpaEntity fromDomainModel(ProductOption option, ProductJpaEntity product) {
        return ProductOptionJpaEntity.builder()
                .id(option.getId())
                .product(product)
                .name(option.getName())
                .priceAmount(option.getPrice().amount())
                .priceCurrency(option.getPrice().currency())
                .bundle(option.isBundle())
                .skuMapping(serializeSkuMapping(option.getSkuMapping()))  // SkuMapping을 JSON으로 직렬화
                .build();
    }
    
    public ProductOption toDomainModel() {
        throw new UnsupportedOperationException("ProductOption should be created through Product domain model");
    }
    
    private static String serializeSkuMapping(SkuMapping skuMapping) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(skuMapping.mappings());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize SkuMapping", e);
        }
    }
    
    private static SkuMapping deserializeSkuMapping(String skuMappingJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Integer> mappings = mapper.readValue(skuMappingJson, new TypeReference<Map<String, Integer>>() {});
            return SkuMapping.of(mappings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize SkuMapping", e);
        }
    }
}