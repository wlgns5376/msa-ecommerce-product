package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.*;
import com.commerce.product.domain.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    private CategoryId categoryId;
    private CategoryName categoryName;

    @BeforeEach
    void setUp() {
        categoryId = new CategoryId("CAT001");
        categoryName = new CategoryName("전자제품");
    }

    @Test
    @DisplayName("최상위 카테고리를 생성할 수 있다")
    void shouldCreateRootCategory() {
        // When
        Category category = Category.createRoot(categoryId, categoryName, 1);

        // Then
        assertThat(category).isNotNull();
        assertThat(category.getId()).isEqualTo(categoryId);
        assertThat(category.getName()).isEqualTo(categoryName);
        assertThat(category.getParentId()).isNull();
        assertThat(category.getLevel()).isEqualTo(1);
        assertThat(category.getSortOrder()).isEqualTo(1);
        assertThat(category.isActive()).isTrue();
        assertThat(category.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("하위 카테고리를 생성할 수 있다")
    void shouldCreateChildCategory() {
        // Given
        CategoryId parentId = new CategoryId("PARENT001");
        
        // When
        Category category = Category.createChild(categoryId, categoryName, parentId, 2, 1);

        // Then
        assertThat(category.getParentId()).isEqualTo(parentId);
        assertThat(category.getLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("카테고리에 하위 카테고리를 추가할 수 있다")
    void shouldAddChildCategory() {
        // Given
        Category parent = Category.createRoot(categoryId, categoryName, 1);
        Category child = Category.createChild(
                new CategoryId("CAT002"),
                new CategoryName("노트북"),
                categoryId,
                2,
                1
        );

        // When
        parent.addChild(child);

        // Then
        assertThat(parent.getChildren()).hasSize(1);
        assertThat(parent.getChildren()).contains(child);
        assertThat(child.getParentId()).isEqualTo(categoryId);
        assertThat(child.getLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("3단계 깊이까지 카테고리를 생성할 수 있다")
    void shouldCreateCategoryUpTo3Levels() {
        // Given
        Category level1 = Category.createRoot(
                new CategoryId("CAT001"),
                new CategoryName("전자제품"),
                1
        );
        Category level2 = Category.createChild(
                new CategoryId("CAT002"),
                new CategoryName("컴퓨터"),
                level1.getId(),
                2,
                1
        );
        Category level3 = Category.createChild(
                new CategoryId("CAT003"),
                new CategoryName("노트북"),
                level2.getId(),
                3,
                1
        );

        // When
        level1.addChild(level2);
        level2.addChild(level3);

        // Then
        assertThat(level1.getLevel()).isEqualTo(1);
        assertThat(level2.getLevel()).isEqualTo(2);
        assertThat(level3.getLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("3단계를 초과하는 카테고리 생성시 예외가 발생한다")
    void shouldThrowExceptionWhenCreatingCategoryBeyond3Levels() {
        // When & Then
        assertThatThrownBy(() -> Category.createChild(
                new CategoryId("CAT004"),
                new CategoryName("하위카테고리"),
                new CategoryId("CAT003"),
                4,
                1
        ))
                .isInstanceOf(InvalidCategoryLevelException.class)
                .hasMessageContaining("Maximum category level is 3");
    }

    @Test
    @DisplayName("3단계 카테고리에 하위 카테고리 추가시 예외가 발생한다")
    void shouldThrowExceptionWhenAddingChildToLevel3Category() {
        // Given
        Category level1 = Category.createRoot(
                new CategoryId("CAT001"),
                new CategoryName("전자제품"),
                1
        );
        Category level2 = Category.createChild(
                new CategoryId("CAT002"),
                new CategoryName("컴퓨터"),
                level1.getId(),
                2,
                1
        );
        Category level3 = Category.createChild(
                new CategoryId("CAT003"),
                new CategoryName("노트북"),
                level2.getId(),
                3,
                1
        );
        
        level1.addChild(level2);
        level2.addChild(level3);
        
        // 레벨 3에 하위 카테고리를 추가하려고 할 때의 자식 카테고리
        // 실제로는 레벨 2로 생성해서 addChild에서 검증
        Category child = Category.createChild(
                new CategoryId("CAT004"),
                new CategoryName("게이밍 노트북"),
                level3.getId(),
                2,  // 일단 레벨 2로 생성
                1
        );

        // When & Then
        assertThatThrownBy(() -> level3.addChild(child))
                .isInstanceOf(MaxCategoryDepthException.class)
                .hasMessageContaining("Cannot add child to level 3 category");
    }

    @Test
    @DisplayName("카테고리를 활성화할 수 있다")
    void shouldActivateCategory() {
        // Given
        Category category = Category.createRoot(categoryId, categoryName, 1);
        category.deactivate(); // 먼저 비활성화

        // When
        category.activate();

        // Then
        assertThat(category.isActive()).isTrue();
        assertThat(category.getDomainEvents()).hasSize(2); // Deactivated + Activated
        assertThat(category.getDomainEvents().get(1)).isInstanceOf(CategoryActivatedEvent.class);
    }

    @Test
    @DisplayName("카테고리를 비활성화할 수 있다")
    void shouldDeactivateCategory() {
        // Given
        Category category = Category.createRoot(categoryId, categoryName, 1);

        // When
        category.deactivate();

        // Then
        assertThat(category.isActive()).isFalse();
        assertThat(category.getDomainEvents()).hasSize(1);
        assertThat(category.getDomainEvents().get(0)).isInstanceOf(CategoryDeactivatedEvent.class);
    }

    @Test
    @DisplayName("활성 상품이 있는 카테고리는 비활성화할 수 없다")
    void shouldThrowExceptionWhenDeactivatingCategoryWithActiveProducts() {
        // Given
        Category category = Category.createRoot(categoryId, categoryName, 1);
        category.setHasActiveProducts(true);

        // When & Then
        assertThatThrownBy(() -> category.deactivate())
                .isInstanceOf(CannotDeactivateCategoryException.class)
                .hasMessageContaining("Cannot deactivate category with active products");
    }

    @Test
    @DisplayName("카테고리 이름을 변경할 수 있다")
    void shouldUpdateCategoryName() {
        // Given
        Category category = Category.createRoot(categoryId, categoryName, 1);
        CategoryName newName = new CategoryName("가전제품");

        // When
        category.updateName(newName);

        // Then
        assertThat(category.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("카테고리 정렬 순서를 변경할 수 있다")
    void shouldUpdateSortOrder() {
        // Given
        Category category = Category.createRoot(categoryId, categoryName, 1);

        // When
        category.updateSortOrder(5);

        // Then
        assertThat(category.getSortOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("전체 경로를 가져올 수 있다")
    void shouldGetFullPath() {
        // Given
        Category level1 = Category.createRoot(
                new CategoryId("CAT001"),
                new CategoryName("전자제품"),
                1
        );
        Category level2 = Category.createChild(
                new CategoryId("CAT002"),
                new CategoryName("컴퓨터"),
                level1.getId(),
                2,
                1
        );
        Category level3 = Category.createChild(
                new CategoryId("CAT003"),
                new CategoryName("노트북"),
                level2.getId(),
                3,
                1
        );

        level1.addChild(level2);
        level2.addChild(level3);

        // When
        String fullPath = level3.getFullPath();

        // Then
        assertThat(fullPath).isEqualTo("전자제품 > 컴퓨터 > 노트북");
    }

    @Test
    @DisplayName("부모 카테고리 참조를 설정할 수 있다")
    void shouldSetParentReference() {
        // Given
        Category parent = Category.createRoot(categoryId, categoryName, 1);
        Category child = Category.createChild(
                new CategoryId("CAT002"),
                new CategoryName("노트북"),
                categoryId,
                2,
                1
        );

        // When
        child.setParent(parent);

        // Then
        assertThat(child.getParent()).isEqualTo(parent);
    }

    @Test
    @DisplayName("자기 자신을 부모로 설정할 수 없다")
    void shouldThrowExceptionWhenSettingSelfAsParent() {
        // Given
        Category category = Category.createRoot(categoryId, categoryName, 1);

        // When & Then
        assertThatThrownBy(() -> category.setParent(category))
                .isInstanceOf(InvalidCategoryHierarchyException.class)
                .hasMessageContaining("Category cannot be its own parent");
    }
    
    @Test
    @DisplayName("하위 카테고리 리스트는 불변으로 반환된다")
    void shouldReturnUnmodifiableChildrenList() {
        // Given
        Category parent = Category.createRoot(categoryId, categoryName, 1);
        Category child = Category.createChild(
                new CategoryId("CAT002"),
                new CategoryName("노트북"),
                categoryId,
                2,
                1
        );
        parent.addChild(child);
        
        // When & Then
        assertThatThrownBy(() -> parent.getChildren().add(child))
                .isInstanceOf(UnsupportedOperationException.class);
                
        assertThatThrownBy(() -> parent.getChildren().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}