package com.commerce.product.infrastructure.persistence.entity;

import com.commerce.product.domain.model.CategoryId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductCategoryJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;
    
    @Column(name = "category_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private String categoryId;
    
    public static ProductCategoryJpaEntity fromDomainModel(CategoryId categoryId, ProductJpaEntity product) {
        return ProductCategoryJpaEntity.builder()
                .product(product)
                .categoryId(categoryId.value())
                .build();
    }
    
    public CategoryId toDomainModel() {
        return new CategoryId(categoryId);
    }
}