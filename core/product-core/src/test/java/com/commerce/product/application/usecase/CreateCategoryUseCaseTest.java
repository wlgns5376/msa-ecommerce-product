package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.CategoryName;
import com.commerce.product.domain.repository.CategoryRepository;
import com.commerce.product.domain.exception.InvalidCategoryIdException;
import com.commerce.product.domain.exception.InvalidCategoryNameException;
import com.commerce.product.domain.exception.InvalidCategoryLevelException;
import com.commerce.product.domain.exception.ProductDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCategoryUseCase 테스트")
class CreateCategoryUseCaseTest {

    @InjectMocks
    private CreateCategoryService createCategoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateCategoryRequest request;

    @BeforeEach
    void setUp() {
        request = null;
    }

    @Test
    @DisplayName("루트 카테고리를 생성할 수 있다")
    void should_create_root_category() {
        // Given
        String categoryId = UUID.randomUUID().toString();
        request = CreateCategoryRequest.builder()
                .name("전자제품")
                .parentId(null)
                .sortOrder(1)
                .build();

        Category expectedCategory = Category.createRoot(
                new CategoryId(categoryId),
                new CategoryName("전자제품"),
                1
        );
        
        when(categoryRepository.save(any(Category.class))).thenReturn(expectedCategory);

        // When
        CreateCategoryResponse response = createCategoryService.createCategory(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("전자제품");
        assertThat(response.getParentId()).isNull();
        assertThat(response.getLevel()).isEqualTo(1);
        assertThat(response.getSortOrder()).isEqualTo(1);
        assertThat(response.isActive()).isTrue();
        
        verify(categoryRepository, times(1)).save(any(Category.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("하위 카테고리를 생성할 수 있다")
    void should_create_child_category() {
        // Given
        String parentId = UUID.randomUUID().toString();
        String categoryId = UUID.randomUUID().toString();
        
        Category parentCategory = Category.createRoot(
                new CategoryId(parentId),
                new CategoryName("전자제품"),
                1
        );
        
        request = CreateCategoryRequest.builder()
                .name("노트북")
                .parentId(parentId)
                .sortOrder(1)
                .build();

        Category expectedCategory = Category.createChild(
                new CategoryId(categoryId),
                new CategoryName("노트북"),
                new CategoryId(parentId),
                2,
                1
        );
        
        when(categoryRepository.findById(new CategoryId(parentId)))
                .thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(expectedCategory);

        // When
        CreateCategoryResponse response = createCategoryService.createCategory(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("노트북");
        assertThat(response.getParentId()).isEqualTo(parentId);
        assertThat(response.getLevel()).isEqualTo(2);
        assertThat(response.getSortOrder()).isEqualTo(1);
        assertThat(response.isActive()).isTrue();
        
        verify(categoryRepository, times(1)).findById(new CategoryId(parentId));
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("3단계 하위 카테고리를 생성할 수 있다")
    void should_create_third_level_category() {
        // Given
        String rootId = UUID.randomUUID().toString();
        String parentId = UUID.randomUUID().toString();
        String categoryId = UUID.randomUUID().toString();
        
        Category secondLevelCategory = Category.createChild(
                new CategoryId(parentId),
                new CategoryName("노트북"),
                new CategoryId(rootId),
                2,
                1
        );
        
        request = CreateCategoryRequest.builder()
                .name("게이밍 노트북")
                .parentId(parentId)
                .sortOrder(1)
                .build();

        Category expectedCategory = Category.createChild(
                new CategoryId(categoryId),
                new CategoryName("게이밍 노트북"),
                new CategoryId(parentId),
                3,
                1
        );
        
        when(categoryRepository.findById(new CategoryId(parentId)))
                .thenReturn(Optional.of(secondLevelCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(expectedCategory);

        // When
        CreateCategoryResponse response = createCategoryService.createCategory(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("게이밍 노트북");
        assertThat(response.getParentId()).isEqualTo(parentId);
        assertThat(response.getLevel()).isEqualTo(3);
        assertThat(response.getSortOrder()).isEqualTo(1);
        assertThat(response.isActive()).isTrue();
        
        verify(categoryRepository, times(1)).findById(new CategoryId(parentId));
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("3단계를 초과하는 카테고리는 생성할 수 없다")
    void should_not_create_category_exceeding_max_level() {
        // Given
        String parentId = UUID.randomUUID().toString();
        
        Category thirdLevelCategory = Category.createChild(
                new CategoryId(parentId),
                new CategoryName("게이밍 노트북"),
                new CategoryId(UUID.randomUUID().toString()),
                3,
                1
        );
        
        request = CreateCategoryRequest.builder()
                .name("ASUS 게이밍")
                .parentId(parentId)
                .sortOrder(1)
                .build();
        
        when(categoryRepository.findById(new CategoryId(parentId)))
                .thenReturn(Optional.of(thirdLevelCategory));

        // When & Then
        assertThatThrownBy(() -> createCategoryService.createCategory(request))
                .isInstanceOf(InvalidCategoryLevelException.class)
                .hasMessageContaining("Maximum category level is");
        
        verify(categoryRepository, times(1)).findById(new CategoryId(parentId));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("존재하지 않는 부모 카테고리로는 하위 카테고리를 생성할 수 없다")
    void should_not_create_category_with_non_existent_parent() {
        // Given
        String parentId = UUID.randomUUID().toString();
        
        request = CreateCategoryRequest.builder()
                .name("노트북")
                .parentId(parentId)
                .sortOrder(1)
                .build();
        
        when(categoryRepository.findById(new CategoryId(parentId)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> createCategoryService.createCategory(request))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("Parent category not found");
        
        verify(categoryRepository, times(1)).findById(new CategoryId(parentId));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("빈 이름으로 카테고리를 생성할 수 없다")
    void should_not_create_category_with_empty_name() {
        // Given
        request = CreateCategoryRequest.builder()
                .name("")
                .parentId(null)
                .sortOrder(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createCategoryService.createCategory(request))
                .isInstanceOf(InvalidCategoryNameException.class)
                .hasMessageContaining("Category name cannot be");
        
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("null 이름으로 카테고리를 생성할 수 없다")
    void should_not_create_category_with_null_name() {
        // Given
        request = CreateCategoryRequest.builder()
                .name(null)
                .parentId(null)
                .sortOrder(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createCategoryService.createCategory(request))
                .isInstanceOf(InvalidCategoryNameException.class)
                .hasMessageContaining("Category name cannot be");
        
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("너무 긴 이름으로 카테고리를 생성할 수 없다")
    void should_not_create_category_with_too_long_name() {
        // Given
        String longName = "a".repeat(101);
        request = CreateCategoryRequest.builder()
                .name(longName)
                .parentId(null)
                .sortOrder(1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createCategoryService.createCategory(request))
                .isInstanceOf(InvalidCategoryNameException.class)
                .hasMessageContaining("Category name cannot exceed");
        
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("음수 정렬 순서로 카테고리를 생성할 수 없다")
    void should_not_create_category_with_negative_sort_order() {
        // Given
        request = CreateCategoryRequest.builder()
                .name("전자제품")
                .parentId(null)
                .sortOrder(-1)
                .build();

        // When & Then
        assertThatThrownBy(() -> createCategoryService.createCategory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sort order must be positive");
        
        verify(categoryRepository, never()).save(any(Category.class));
    }
}