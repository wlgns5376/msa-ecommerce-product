package com.commerce.product.infrastructure.persistence.entity;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.CategoryName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryJpaEntityTest {
    
    @Test
    @DisplayName("도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void fromDomainModel() {
        // Given
        CategoryId categoryId = new CategoryId(UUID.randomUUID().toString());
        CategoryName categoryName = new CategoryName("전자제품");
        Category category = Category.createRoot(categoryId, categoryName, 0);
        
        // When
        CategoryJpaEntity entity = CategoryJpaEntity.fromDomainModel(category);
        
        // Then
        assertThat(entity.getId()).isEqualTo(categoryId.value());
        assertThat(entity.getName()).isEqualTo(categoryName.value());
        assertThat(entity.getParentId()).isNull();
        assertThat(entity.getLevel()).isEqualTo(1);
        assertThat(entity.getSortOrder()).isEqualTo(0);
        assertThat(entity.getIsActive()).isTrue();
        assertThat(entity.getVersion()).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("JPA 엔티티를 도메인 모델로 변환할 수 있다")
    void toDomainModel() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        CategoryJpaEntity entity = CategoryJpaEntity.builder()
                .id(categoryId)
                .name("전자제품")
                .parentId(null)
                .level(1)
                .sortOrder(0)
                .isActive(true)
                .version(0L)
                .build();
        
        // When
        Category category = entity.toDomainModel();
        
        // Then
        assertThat(category.getId().value()).isEqualTo(categoryId);
        assertThat(category.getName().value()).isEqualTo("전자제품");
        assertThat(category.getParentId()).isNull();
        assertThat(category.getLevel()).isEqualTo(1);
        assertThat(category.getSortOrder()).isEqualTo(0);
        assertThat(category.isActive()).isTrue();
        // Category 도메인 모델에는 version 필드가 없음
    }
    
    @Test
    @DisplayName("부모 카테고리가 있는 도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void fromDomainModel_WithParent() {
        // Given
        CategoryId parentId = new CategoryId(UUID.randomUUID().toString());
        CategoryId categoryId = new CategoryId(UUID.randomUUID().toString());
        CategoryName categoryName = new CategoryName("노트북");
        Category category = Category.createChild(categoryId, categoryName, parentId, 2, 1);
        
        // When
        CategoryJpaEntity entity = CategoryJpaEntity.fromDomainModel(category);
        
        // Then
        assertThat(entity.getId()).isEqualTo(categoryId.value());
        assertThat(entity.getName()).isEqualTo(categoryName.value());
        assertThat(entity.getParentId()).isEqualTo(parentId.value());
        assertThat(entity.getLevel()).isEqualTo(2);
        assertThat(entity.getSortOrder()).isEqualTo(1);
        assertThat(entity.getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("부모 카테고리가 있는 JPA 엔티티를 도메인 모델로 변환할 수 있다")
    void toDomainModel_WithParent() {
        // Given
        String parentId = UUID.randomUUID().toString();
        String categoryId = UUID.randomUUID().toString();
        CategoryJpaEntity entity = CategoryJpaEntity.builder()
                .id(categoryId)
                .name("노트북")
                .parentId(parentId)
                .level(2)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
        
        // When
        Category category = entity.toDomainModel();
        
        // Then
        assertThat(category.getId().value()).isEqualTo(categoryId);
        assertThat(category.getName().value()).isEqualTo("노트북");
        assertThat(category.getParentId()).isNotNull();
        assertThat(category.getParentId().value()).isEqualTo(parentId);
        assertThat(category.getLevel()).isEqualTo(2);
        assertThat(category.getSortOrder()).isEqualTo(1);
        assertThat(category.isActive()).isTrue();
    }
    
    @Test
    @DisplayName("비활성 카테고리를 변환할 수 있다")
    void convertInactiveCategory() {
        // Given
        CategoryId categoryId = new CategoryId(UUID.randomUUID().toString());
        CategoryName categoryName = new CategoryName("구형모델");
        Category category = Category.createRoot(categoryId, categoryName, 0);
        category.setHasActiveProducts(false); // 활성 상품이 없도록 설정
        category.deactivate(); // 비활성화
        
        // When
        CategoryJpaEntity entity = CategoryJpaEntity.fromDomainModel(category);
        
        // Then
        assertThat(entity.getIsActive()).isFalse();
        
        // toDomainModelWithChildren을 사용해야 비활성 상태가 복원됨
        Category restoredCategory = entity.toDomainModelWithChildren();
        assertThat(restoredCategory.isActive()).isFalse();
    }
}