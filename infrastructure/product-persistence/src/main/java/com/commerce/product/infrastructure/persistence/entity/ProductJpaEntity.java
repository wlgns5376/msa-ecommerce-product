package com.commerce.product.infrastructure.persistence.entity;

import com.commerce.product.domain.model.*;
import com.commerce.product.infrastructure.persistence.common.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductJpaEntity extends BaseJpaEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductType type;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    
    @Column(name = "is_out_of_stock", nullable = false)
    @Builder.Default
    private boolean outOfStock = false;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductOptionJpaEntity> options = new ArrayList<>();
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductCategoryJpaEntity> categories = new ArrayList<>();
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    public static ProductJpaEntity fromDomainModel(Product product) {
        ProductJpaEntity entity = ProductJpaEntity.builder()
                .id(product.getId().value())
                .name(product.getName().value())
                .description(product.getDescription())
                .type(product.getType())
                .status(product.getStatus())
                .outOfStock(product.isOutOfStock())
                .version(product.getVersion())
                .build();
        
        // 옵션 설정
        List<ProductOptionJpaEntity> optionEntities = product.getOptions().stream()
                .map(option -> ProductOptionJpaEntity.fromDomainModel(option, entity))
                .collect(Collectors.toList());
        entity.options.clear();
        entity.options.addAll(optionEntities);
        
        // 카테고리 설정
        List<ProductCategoryJpaEntity> categoryEntities = product.getCategoryIds().stream()
                .map(categoryId -> ProductCategoryJpaEntity.fromDomainModel(categoryId, entity))
                .collect(Collectors.toList());
        entity.categories.clear();
        entity.categories.addAll(categoryEntities);
        
        return entity;
    }
    
    public Product toDomainModel() {
        // 옵션 복원을 위한 임시 리스트
        List<ProductOption> productOptions = options.stream()
                .map(ProductOptionJpaEntity::toDomainModel)
                .collect(Collectors.toList());
        
        // 카테고리 복원
        List<CategoryId> categoryIds = categories.stream()
                .map(ProductCategoryJpaEntity::toDomainModel)
                .collect(Collectors.toList());
        
        // restore 메서드를 사용하여 version 포함해서 복원
        Product product = Product.restore(
                new ProductId(id),
                new ProductName(name),
                description,
                type,
                status,
                productOptions,
                categoryIds,
                outOfStock,
                version
        );
        
        return product;
    }
    
    public Long getVersion() {
        return version;
    }
}