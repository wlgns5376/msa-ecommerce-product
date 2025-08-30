package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.CategoryName;
import com.commerce.product.domain.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCategoryTreeUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private GetCategoryTreeUseCase getCategoryTreeUseCase;

    @BeforeEach
    void setUp() {
        getCategoryTreeUseCase = new GetCategoryTreeService(categoryRepository);
    }

    @Test
    @DisplayName("전체 카테고리 트리를 조회할 수 있다")
    void shouldGetFullCategoryTree() {
        // Given
        String rootId1 = UUID.randomUUID().toString();
        String rootId2 = UUID.randomUUID().toString();
        String childId1 = UUID.randomUUID().toString();
        String childId2 = UUID.randomUUID().toString();
        String grandChildId = UUID.randomUUID().toString();

        Category root1 = Category.createRoot(
                CategoryId.of(rootId1),
                CategoryName.of("전자제품"),
                1
        );

        Category root2 = Category.createRoot(
                CategoryId.of(rootId2),
                CategoryName.of("의류"),
                2
        );

        Category child1 = Category.createChild(
                CategoryId.of(childId1),
                CategoryName.of("노트북"),
                CategoryId.of(rootId1),
                2,
                1
        );

        Category child2 = Category.createChild(
                CategoryId.of(childId2),
                CategoryName.of("스마트폰"),
                CategoryId.of(rootId1),
                2,
                2
        );

        Category grandChild = Category.createChild(
                CategoryId.of(grandChildId),
                CategoryName.of("게이밍 노트북"),
                CategoryId.of(childId1),
                3,
                1
        );

        GetCategoryTreeRequest request = GetCategoryTreeRequest.builder()
                .includeInactive(false)
                .build();

        when(categoryRepository.findRootCategories()).thenReturn(Arrays.asList(root1, root2));
        when(categoryRepository.findByParentId(CategoryId.of(rootId1)))
                .thenReturn(Arrays.asList(child1, child2));
        when(categoryRepository.findByParentId(CategoryId.of(rootId2)))
                .thenReturn(List.of());
        when(categoryRepository.findByParentId(CategoryId.of(childId1)))
                .thenReturn(List.of(grandChild));
        when(categoryRepository.findByParentId(CategoryId.of(childId2)))
                .thenReturn(List.of());
        when(categoryRepository.findByParentId(CategoryId.of(grandChildId)))
                .thenReturn(List.of());

        // When
        GetCategoryTreeResponse response = getCategoryTreeUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategories()).hasSize(2);
        
        CategoryTreeNode electronics = response.getCategories().get(0);
        assertThat(electronics.getName()).isEqualTo("전자제품");
        assertThat(electronics.getChildren()).hasSize(2);
        
        CategoryTreeNode laptop = electronics.getChildren().get(0);
        assertThat(laptop.getName()).isEqualTo("노트북");
        assertThat(laptop.getLevel()).isEqualTo(2);
        assertThat(laptop.getChildren()).hasSize(1);
        
        CategoryTreeNode gamingLaptop = laptop.getChildren().get(0);
        assertThat(gamingLaptop.getName()).isEqualTo("게이밍 노트북");
        assertThat(gamingLaptop.getLevel()).isEqualTo(3);
        assertThat(gamingLaptop.getChildren()).isEmpty();

        verify(categoryRepository, times(1)).findRootCategories();
        verify(categoryRepository, atLeastOnce()).findByParentId(any());
    }

    @Test
    @DisplayName("활성 카테고리만 조회할 수 있다")
    void shouldGetOnlyActiveCategories() {
        // Given
        String rootId = UUID.randomUUID().toString();
        String activeChildId = UUID.randomUUID().toString();
        String inactiveChildId = UUID.randomUUID().toString();

        Category root = Category.createRoot(
                CategoryId.of(rootId),
                CategoryName.of("전자제품"),
                1
        );

        Category activeChild = Category.createChild(
                CategoryId.of(activeChildId),
                CategoryName.of("노트북"),
                CategoryId.of(rootId),
                2,
                1
        );

        Category inactiveChild = Category.createChild(
                CategoryId.of(inactiveChildId),
                CategoryName.of("데스크탑"),
                CategoryId.of(rootId),
                2,
                2
        );
        inactiveChild.deactivate();

        GetCategoryTreeRequest request = GetCategoryTreeRequest.builder()
                .includeInactive(false)
                .build();

        when(categoryRepository.findRootCategories()).thenReturn(List.of(root));
        when(categoryRepository.findByParentId(CategoryId.of(rootId)))
                .thenReturn(Arrays.asList(activeChild, inactiveChild));
        when(categoryRepository.findByParentId(CategoryId.of(activeChildId)))
                .thenReturn(List.of());

        // When
        GetCategoryTreeResponse response = getCategoryTreeUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategories()).hasSize(1);
        
        CategoryTreeNode electronics = response.getCategories().get(0);
        assertThat(electronics.getChildren()).hasSize(1);
        assertThat(electronics.getChildren().get(0).getName()).isEqualTo("노트북");
    }

    @Test
    @DisplayName("비활성 카테고리를 포함하여 조회할 수 있다")
    void shouldGetCategoriesIncludingInactive() {
        // Given
        String rootId = UUID.randomUUID().toString();
        String activeChildId = UUID.randomUUID().toString();
        String inactiveChildId = UUID.randomUUID().toString();

        Category root = Category.createRoot(
                CategoryId.of(rootId),
                CategoryName.of("전자제품"),
                1
        );

        Category activeChild = Category.createChild(
                CategoryId.of(activeChildId),
                CategoryName.of("노트북"),
                CategoryId.of(rootId),
                2,
                1
        );

        Category inactiveChild = Category.createChild(
                CategoryId.of(inactiveChildId),
                CategoryName.of("데스크탑"),
                CategoryId.of(rootId),
                2,
                2
        );
        inactiveChild.deactivate();

        GetCategoryTreeRequest request = GetCategoryTreeRequest.builder()
                .includeInactive(true)
                .build();

        when(categoryRepository.findRootCategories()).thenReturn(List.of(root));
        when(categoryRepository.findByParentId(CategoryId.of(rootId)))
                .thenReturn(Arrays.asList(activeChild, inactiveChild));
        when(categoryRepository.findByParentId(CategoryId.of(activeChildId)))
                .thenReturn(List.of());
        when(categoryRepository.findByParentId(CategoryId.of(inactiveChildId)))
                .thenReturn(List.of());

        // When
        GetCategoryTreeResponse response = getCategoryTreeUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategories()).hasSize(1);
        
        CategoryTreeNode electronics = response.getCategories().get(0);
        assertThat(electronics.getChildren()).hasSize(2);
        assertThat(electronics.getChildren().stream()
                .map(CategoryTreeNode::getName))
                .containsExactlyInAnyOrder("노트북", "데스크탑");
    }

    @Test
    @DisplayName("빈 카테고리 트리를 조회할 수 있다")
    void shouldGetEmptyCategoryTree() {
        // Given
        GetCategoryTreeRequest request = GetCategoryTreeRequest.builder()
                .includeInactive(false)
                .build();

        when(categoryRepository.findRootCategories()).thenReturn(List.of());

        // When
        GetCategoryTreeResponse response = getCategoryTreeUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategories()).isEmpty();

        verify(categoryRepository, times(1)).findRootCategories();
        verify(categoryRepository, never()).findByParentId(any());
    }

    @Test
    @DisplayName("정렬 순서에 따라 카테고리를 정렬하여 반환한다")
    void shouldReturnCategoriesSortedBySortOrder() {
        // Given
        String rootId = UUID.randomUUID().toString();
        String childId1 = UUID.randomUUID().toString();
        String childId2 = UUID.randomUUID().toString();
        String childId3 = UUID.randomUUID().toString();

        Category root = Category.createRoot(
                CategoryId.of(rootId),
                CategoryName.of("전자제품"),
                1
        );

        Category child1 = Category.createChild(
                CategoryId.of(childId1),
                CategoryName.of("스마트폰"),
                CategoryId.of(rootId),
                2,
                3
        );

        Category child2 = Category.createChild(
                CategoryId.of(childId2),
                CategoryName.of("노트북"),
                CategoryId.of(rootId),
                2,
                1
        );

        Category child3 = Category.createChild(
                CategoryId.of(childId3),
                CategoryName.of("태블릿"),
                CategoryId.of(rootId),
                2,
                2
        );

        GetCategoryTreeRequest request = GetCategoryTreeRequest.builder()
                .includeInactive(false)
                .build();

        when(categoryRepository.findRootCategories()).thenReturn(List.of(root));
        when(categoryRepository.findByParentId(CategoryId.of(rootId)))
                .thenReturn(Arrays.asList(child2, child3, child1)); // 순서 섞어서 반환
        when(categoryRepository.findByParentId(CategoryId.of(childId1)))
                .thenReturn(List.of());
        when(categoryRepository.findByParentId(CategoryId.of(childId2)))
                .thenReturn(List.of());
        when(categoryRepository.findByParentId(CategoryId.of(childId3)))
                .thenReturn(List.of());

        // When
        GetCategoryTreeResponse response = getCategoryTreeUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        CategoryTreeNode electronics = response.getCategories().get(0);
        assertThat(electronics.getChildren()).hasSize(3);
        assertThat(electronics.getChildren().get(0).getName()).isEqualTo("노트북");
        assertThat(electronics.getChildren().get(1).getName()).isEqualTo("태블릿");
        assertThat(electronics.getChildren().get(2).getName()).isEqualTo("스마트폰");
    }
}