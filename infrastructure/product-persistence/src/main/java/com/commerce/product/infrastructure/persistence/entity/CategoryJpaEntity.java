package com.commerce.product.infrastructure.persistence.entity;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.CategoryName;
import com.commerce.product.infrastructure.persistence.common.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class CategoryJpaEntity extends BaseJpaEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "parent_id", columnDefinition = "VARCHAR(36)")
    private String parentId;
    
    @Column(name = "level", nullable = false)
    private Integer level;
    
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private CategoryJpaEntity parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, name ASC")
    @Builder.Default
    private List<CategoryJpaEntity> children = new ArrayList<>();
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    public static CategoryJpaEntity fromDomainModel(Category category) {
        CategoryJpaEntity entity = CategoryJpaEntity.builder()
                .id(category.getId().value())
                .name(category.getName().value())
                .parentId(category.getParentId() != null ? category.getParentId().value() : null)
                .level(category.getLevel())
                .sortOrder(category.getSortOrder())
                .isActive(category.isActive())
                .version(0L) // 새로운 엔티티는 version 0으로 시작
                .build();
        
        // 자식 카테고리들은 별도로 저장되므로 여기서는 설정하지 않음
        
        return entity;
    }
    
    public Category toDomainModel() {
        if (parentId != null) {
            return Category.createChild(
                    new CategoryId(id),
                    new CategoryName(name),
                    new CategoryId(parentId),
                    level,
                    sortOrder
            );
        } else {
            return Category.createRoot(
                    new CategoryId(id),
                    new CategoryName(name),
                    sortOrder
            );
        }
    }
    
    public Category toDomainModelWithChildren() {
        Category category = toDomainModel();
        
        // 비활성 상태 처리
        if (!isActive) {
            category.deactivate();
        }
        
        // 자식 카테고리들을 재귀적으로 변환하여 추가
        if (children != null && !children.isEmpty()) {
            for (CategoryJpaEntity childEntity : children) {
                Category childCategory = childEntity.toDomainModelWithChildren();
                category.addChild(childCategory);
            }
        }
        
        return category;
    }
}