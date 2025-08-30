package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.InvalidCategoryIdException;
import com.commerce.product.domain.exception.InvalidCategoryNameException;
import com.commerce.product.domain.exception.MaxCategoryDepthException;
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
class CreateCategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CreateCategoryUseCase createCategoryUseCase;

    @BeforeEach
    void setUp() {
        createCategoryUseCase = new CreateCategoryService(categoryRepository);
    }

    @Test
    @DisplayName("루트 카테고리를 생성할 수 있다")
    void shouldCreateRootCategory() {
        // Given
        String categoryName = "전자제품";
        int sortOrder = 1;
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(categoryName)
                .parentId(null)
                .sortOrder(sortOrder)
                .build();

        Category savedCategory = Category.createRoot(
                CategoryId.generate(),
                CategoryName.of(categoryName),
                sortOrder
        );

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        CreateCategoryResponse response = createCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isNotNull();
        assertThat(response.getName()).isEqualTo(categoryName);
        assertThat(response.getLevel()).isEqualTo(1);
        assertThat(response.getParentId()).isNull();
        assertThat(response.getSortOrder()).isEqualTo(sortOrder);
        assertThat(response.isActive()).isTrue();

        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("하위 카테고리를 생성할 수 있다")
    void shouldCreateChildCategory() {
        // Given
        String parentId = UUID.randomUUID().toString();
        String categoryName = "노트북";
        int sortOrder = 1;
        
        Category parentCategory = Category.createRoot(
                CategoryId.of(parentId),
                CategoryName.of("전자제품"),
                1
        );

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(categoryName)
                .parentId(parentId)
                .sortOrder(sortOrder)
                .build();

        when(categoryRepository.findById(CategoryId.of(parentId)))
                .thenReturn(Optional.of(parentCategory));

        Category savedCategory = Category.createChild(
                CategoryId.generate(),
                CategoryName.of(categoryName),
                CategoryId.of(parentId),
                2,
                sortOrder
        );

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        CreateCategoryResponse response = createCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryId()).isNotNull();
        assertThat(response.getName()).isEqualTo(categoryName);
        assertThat(response.getLevel()).isEqualTo(2);
        assertThat(response.getParentId()).isEqualTo(parentId);
        assertThat(response.getSortOrder()).isEqualTo(sortOrder);
        assertThat(response.isActive()).isTrue();

        verify(categoryRepository, times(1)).findById(CategoryId.of(parentId));
        verify(categoryRepository, times(2)).save(any(Category.class)); // 부모와 자식 카테고리 모두 저장
    }

    @Test
    @DisplayName("3단계 하위 카테고리를 생성할 수 있다")
    void shouldCreateThirdLevelCategory() {
        // Given
        String rootId = UUID.randomUUID().toString();
        String parentId = UUID.randomUUID().toString();
        String categoryName = "게이밍 노트북";
        int sortOrder = 1;
        
        Category rootCategory = Category.createRoot(
                CategoryId.of(rootId),
                CategoryName.of("전자제품"),
                1
        );
        
        Category parentCategory = Category.createChild(
                CategoryId.of(parentId),
                CategoryName.of("노트북"),
                CategoryId.of(rootId),
                2,
                1
        );

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(categoryName)
                .parentId(parentId)
                .sortOrder(sortOrder)
                .build();

        when(categoryRepository.findById(CategoryId.of(parentId)))
                .thenReturn(Optional.of(parentCategory));

        Category savedCategory = Category.createChild(
                CategoryId.generate(),
                CategoryName.of(categoryName),
                CategoryId.of(parentId),
                3,
                sortOrder
        );

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        CreateCategoryResponse response = createCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getLevel()).isEqualTo(3);
        assertThat(response.getParentId()).isEqualTo(parentId);
        
        verify(categoryRepository, times(2)).save(any(Category.class)); // 부모와 자식 카테고리 모두 저장
    }

    @Test
    @DisplayName("3단계 카테고리에는 하위 카테고리를 생성할 수 없다")
    void shouldNotCreateChildForThirdLevelCategory() {
        // Given
        String parentId = UUID.randomUUID().toString();
        
        Category thirdLevelCategory = Category.createChild(
                CategoryId.of(parentId),
                CategoryName.of("게이밍 노트북"),
                CategoryId.of(UUID.randomUUID().toString()),
                3,
                1
        );

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("하위 카테고리")
                .parentId(parentId)
                .sortOrder(1)
                .build();

        when(categoryRepository.findById(CategoryId.of(parentId)))
                .thenReturn(Optional.of(thirdLevelCategory));

        // When & Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(request))
                .isInstanceOf(MaxCategoryDepthException.class)
                .hasMessageContaining("Maximum category depth");

        verify(categoryRepository, times(1)).findById(CategoryId.of(parentId));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("존재하지 않는 부모 카테고리로 하위 카테고리를 생성할 수 없다")
    void shouldNotCreateChildWithNonExistentParent() {
        // Given
        String parentId = UUID.randomUUID().toString();

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("노트북")
                .parentId(parentId)
                .sortOrder(1)
                .build();

        when(categoryRepository.findById(CategoryId.of(parentId)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(request))
                .isInstanceOf(InvalidCategoryIdException.class)
                .hasMessageContaining("Parent category not found");

        verify(categoryRepository, times(1)).findById(CategoryId.of(parentId));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("빈 이름으로 카테고리를 생성할 수 없다")
    void shouldNotCreateCategoryWithEmptyName() {
        // Given
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("")
                .parentId(null)
                .sortOrder(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(request))
                .isInstanceOf(InvalidCategoryNameException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("null 이름으로 카테고리를 생성할 수 없다")
    void shouldNotCreateCategoryWithNullName() {
        // Given
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(null)
                .parentId(null)
                .sortOrder(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(request))
                .isInstanceOf(InvalidCategoryNameException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("너무 긴 이름으로 카테고리를 생성할 수 없다")
    void shouldNotCreateCategoryWithTooLongName() {
        // Given
        String longName = "a".repeat(101); // 100자 초과
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(longName)
                .parentId(null)
                .sortOrder(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(request))
                .isInstanceOf(InvalidCategoryNameException.class)
                .hasMessageContaining("100 characters");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("음수 정렬 순서로 카테고리를 생성할 수 없다")
    void shouldNotCreateCategoryWithNegativeSortOrder() {
        // Given
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("전자제품")
                .parentId(null)
                .sortOrder(-1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sort order must be non-negative");

        verify(categoryRepository, never()).save(any(Category.class));
    }
}