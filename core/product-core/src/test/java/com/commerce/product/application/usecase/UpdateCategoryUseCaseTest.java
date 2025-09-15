package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.CannotDeactivateCategoryException;
import com.commerce.product.domain.exception.InvalidCategoryIdException;
import com.commerce.product.domain.exception.InvalidCategoryNameException;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private UpdateCategoryUseCase updateCategoryUseCase;

    @BeforeEach
    void setUp() {
        updateCategoryUseCase = new UpdateCategoryService(categoryRepository);
    }

    @Test
    @DisplayName("카테고리 이름을 수정할 수 있다")
    void shouldUpdateCategoryName() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        String newName = "노트북 및 액세서리";
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("노트북"),
                1
        );

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .name(newName)
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        UpdateCategoryResponse response = updateCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(categoryId);
        assertThat(response.getName()).isEqualTo(newName);
        assertThat(response.isActive()).isTrue();

        verify(categoryRepository, times(1)).findById(CategoryId.of(categoryId));
        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    @DisplayName("카테고리 정렬 순서를 수정할 수 있다")
    void shouldUpdateCategorySortOrder() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        int newSortOrder = 5;
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("전자제품"),
                1
        );

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .sortOrder(newSortOrder)
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        UpdateCategoryResponse response = updateCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isEqualTo(categoryId);
        assertThat(response.getSortOrder()).isEqualTo(newSortOrder);

        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    @DisplayName("카테고리를 활성화할 수 있다")
    void shouldActivateCategory() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("전자제품"),
                1
        );
        category.deactivate(); // 먼저 비활성화

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .isActive(true)
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        UpdateCategoryResponse response = updateCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isTrue();

        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    @DisplayName("활성 상품이 없는 카테고리를 비활성화할 수 있다")
    void shouldDeactivateCategoryWithoutActiveProducts() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("전자제품"),
                1
        );

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .isActive(false)
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.of(category));
        when(categoryRepository.hasActiveProducts(CategoryId.of(categoryId)))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        UpdateCategoryResponse response = updateCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isFalse();

        verify(categoryRepository, times(1)).hasActiveProducts(CategoryId.of(categoryId));
        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    @DisplayName("활성 상품이 있는 카테고리는 비활성화할 수 없다")
    void shouldNotDeactivateCategoryWithActiveProducts() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("전자제품"),
                1
        );

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .isActive(false)
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.of(category));
        when(categoryRepository.hasActiveProducts(CategoryId.of(categoryId)))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> updateCategoryUseCase.execute(request))
                .isInstanceOf(CannotDeactivateCategoryException.class)
                .hasMessageContaining("Cannot deactivate category with active products");

        verify(categoryRepository, times(1)).hasActiveProducts(CategoryId.of(categoryId));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("여러 속성을 동시에 수정할 수 있다")
    void shouldUpdateMultipleAttributes() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        String newName = "스마트폰 및 액세서리";
        int newSortOrder = 3;
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("스마트폰"),
                1
        );

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .name(newName)
                .sortOrder(newSortOrder)
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        UpdateCategoryResponse response = updateCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(newName);
        assertThat(response.getSortOrder()).isEqualTo(newSortOrder);

        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    @DisplayName("존재하지 않는 카테고리는 수정할 수 없다")
    void shouldNotUpdateNonExistentCategory() {
        // Given
        String categoryId = UUID.randomUUID().toString();

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .name("새 이름")
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> updateCategoryUseCase.execute(request))
                .isInstanceOf(InvalidCategoryIdException.class)
                .hasMessageContaining("Category not found");

        verify(categoryRepository, times(1)).findById(CategoryId.of(categoryId));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("빈 이름으로 카테고리를 수정할 수 없다")
    void shouldNotUpdateCategoryWithEmptyName() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("전자제품"),
                1
        );

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .name("")
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.of(category));

        // When & Then
        assertThatThrownBy(() -> updateCategoryUseCase.execute(request))
                .isInstanceOf(InvalidCategoryNameException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("음수 정렬 순서로 카테고리를 수정할 수 없다")
    void shouldNotUpdateCategoryWithNegativeSortOrder() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("전자제품"),
                1
        );

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .categoryId(categoryId)
                .sortOrder(-1)
                .build();

        when(categoryRepository.findById(CategoryId.of(categoryId)))
                .thenReturn(Optional.of(category));

        // When & Then
        assertThatThrownBy(() -> updateCategoryUseCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sort order must be non-negative");

        verify(categoryRepository, never()).save(any(Category.class));
    }
}