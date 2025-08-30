package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.InvalidCategoryIdException;
import com.commerce.product.domain.exception.MaxCategoryLimitException;
import com.commerce.product.domain.exception.ProductNotFoundException;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.domain.repository.CategoryRepository;
import com.commerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignProductToCategoryUseCaseTest {

    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private CategoryRepository categoryRepository;

    private AssignProductToCategoryUseCase assignProductToCategoryUseCase;

    @BeforeEach
    void setUp() {
        assignProductToCategoryUseCase = new AssignProductToCategoryService(productRepository, categoryRepository);
    }

    @Test
    @DisplayName("상품을 단일 카테고리에 할당할 수 있다")
    void shouldAssignProductToSingleCategory() {
        // Given
        String productId = UUID.randomUUID().toString();
        String categoryId = UUID.randomUUID().toString();
        
        Product product = Product.restore(
                ProductId.of(productId),
                ProductName.of("노트북"),
                "고성능 노트북",
                ProductType.NORMAL,
                ProductStatus.ACTIVE,
                List.of(),
                List.of(),
                false,
                0L
        );
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("전자제품"),
                1
        );

        AssignProductToCategoryRequest request = AssignProductToCategoryRequest.builder()
                .productId(productId)
                .categoryIds(List.of(categoryId))
                .build();

        when(productRepository.findById(ProductId.of(productId)))
                .thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(any()))
                .thenReturn(List.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        AssignProductToCategoryResponse response = assignProductToCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getCategoryIds()).containsExactly(categoryId);
        assertThat(response.isSuccess()).isTrue();

        verify(productRepository, times(1)).findById(ProductId.of(productId));
        verify(categoryRepository, times(1)).findAllById(any());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품을 여러 카테고리에 할당할 수 있다")
    void shouldAssignProductToMultipleCategories() {
        // Given
        String productId = UUID.randomUUID().toString();
        String categoryId1 = UUID.randomUUID().toString();
        String categoryId2 = UUID.randomUUID().toString();
        String categoryId3 = UUID.randomUUID().toString();
        
        Product product = Product.restore(
                ProductId.of(productId),
                ProductName.of("스마트워치"),
                "피트니스 기능이 있는 스마트워치",
                ProductType.NORMAL,
                ProductStatus.ACTIVE,
                List.of(),
                List.of(),
                false,
                0L
        );
        
        Category category1 = Category.createRoot(
                CategoryId.of(categoryId1),
                CategoryName.of("전자제품"),
                1
        );
        
        Category category2 = Category.createRoot(
                CategoryId.of(categoryId2),
                CategoryName.of("웨어러블"),
                2
        );
        
        Category category3 = Category.createRoot(
                CategoryId.of(categoryId3),
                CategoryName.of("피트니스"),
                3
        );

        AssignProductToCategoryRequest request = AssignProductToCategoryRequest.builder()
                .productId(productId)
                .categoryIds(List.of(categoryId1, categoryId2, categoryId3))
                .build();

        when(productRepository.findById(ProductId.of(productId)))
                .thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(any()))
                .thenReturn(List.of(category1, category2, category3));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        AssignProductToCategoryResponse response = assignProductToCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getCategoryIds()).containsExactlyInAnyOrder(categoryId1, categoryId2, categoryId3);
        assertThat(response.isSuccess()).isTrue();

        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품은 최대 5개의 카테고리에만 할당할 수 있다")
    void shouldNotAssignProductToMoreThanFiveCategories() {
        // Given
        String productId = UUID.randomUUID().toString();
        List<String> categoryIds = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString() // 6개
        );

        AssignProductToCategoryRequest request = AssignProductToCategoryRequest.builder()
                .productId(productId)
                .categoryIds(categoryIds)
                .build();

        // When & Then
        assertThatThrownBy(() -> assignProductToCategoryUseCase.execute(request))
                .isInstanceOf(MaxCategoryLimitException.class)
                .hasMessageContaining("최대 5개의 카테고리");

        verify(productRepository, never()).findById(any(ProductId.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("존재하지 않는 상품에는 카테고리를 할당할 수 없다")
    void shouldNotAssignCategoryToNonExistentProduct() {
        // Given
        String productId = UUID.randomUUID().toString();
        String categoryId = UUID.randomUUID().toString();

        AssignProductToCategoryRequest request = AssignProductToCategoryRequest.builder()
                .productId(productId)
                .categoryIds(List.of(categoryId))
                .build();

        when(productRepository.findById(ProductId.of(productId)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> assignProductToCategoryUseCase.execute(request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository, times(1)).findById(ProductId.of(productId));
        verify(categoryRepository, never()).findAllById(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리는 상품에 할당할 수 없다")
    void shouldNotAssignNonExistentCategoryToProduct() {
        // Given
        String productId = UUID.randomUUID().toString();
        String existingCategoryId = UUID.randomUUID().toString();
        String nonExistentCategoryId = UUID.randomUUID().toString();
        
        Product product = Product.restore(
                ProductId.of(productId),
                ProductName.of("노트북"),
                "고성능 노트북",
                ProductType.NORMAL,
                ProductStatus.ACTIVE,
                List.of(),
                List.of(),
                false,
                0L
        );
        
        Category existingCategory = Category.createRoot(
                CategoryId.of(existingCategoryId),
                CategoryName.of("전자제품"),
                1
        );

        AssignProductToCategoryRequest request = AssignProductToCategoryRequest.builder()
                .productId(productId)
                .categoryIds(List.of(existingCategoryId, nonExistentCategoryId))
                .build();

        when(productRepository.findById(ProductId.of(productId)))
                .thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(any()))
                .thenReturn(List.of(existingCategory)); // 하나만 반환

        // When & Then
        assertThatThrownBy(() -> assignProductToCategoryUseCase.execute(request))
                .isInstanceOf(InvalidCategoryIdException.class)
                .hasMessageContaining("Some categories not found");

        verify(productRepository, times(1)).findById(ProductId.of(productId));
        verify(categoryRepository, times(1)).findAllById(any());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("빈 카테고리 목록으로 상품의 카테고리를 모두 제거할 수 있다")
    void shouldRemoveAllCategoriesWhenEmptyListProvided() {
        // Given
        String productId = UUID.randomUUID().toString();
        
        Product product = Product.restore(
                ProductId.of(productId),
                ProductName.of("노트북"),
                "고성능 노트북",
                ProductType.NORMAL,
                ProductStatus.ACTIVE,
                List.of(),
                List.of(),
                false,
                0L
        );
        
        // 기존에 카테고리가 할당되어 있다고 가정
        product.assignCategories(List.of(CategoryId.of(UUID.randomUUID().toString())));

        AssignProductToCategoryRequest request = AssignProductToCategoryRequest.builder()
                .productId(productId)
                .categoryIds(List.of())
                .build();

        when(productRepository.findById(ProductId.of(productId)))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        AssignProductToCategoryResponse response = assignProductToCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getCategoryIds()).isEmpty();
        assertThat(response.isSuccess()).isTrue();

        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("비활성 카테고리도 상품에 할당할 수 있다")
    void shouldAssignInactiveCategoryToProduct() {
        // Given
        String productId = UUID.randomUUID().toString();
        String categoryId = UUID.randomUUID().toString();
        
        Product product = Product.restore(
                ProductId.of(productId),
                ProductName.of("노트북"),
                "고성능 노트북",
                ProductType.NORMAL,
                ProductStatus.ACTIVE,
                List.of(),
                List.of(),
                false,
                0L
        );
        
        Category category = Category.createRoot(
                CategoryId.of(categoryId),
                CategoryName.of("전자제품"),
                1
        );
        category.deactivate(); // 카테고리 비활성화

        AssignProductToCategoryRequest request = AssignProductToCategoryRequest.builder()
                .productId(productId)
                .categoryIds(List.of(categoryId))
                .build();

        when(productRepository.findById(ProductId.of(productId)))
                .thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(any()))
                .thenReturn(List.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        AssignProductToCategoryResponse response = assignProductToCategoryUseCase.execute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCategoryIds()).containsExactly(categoryId);
        assertThat(response.isSuccess()).isTrue();

        verify(productRepository, times(1)).save(any(Product.class));
    }
}