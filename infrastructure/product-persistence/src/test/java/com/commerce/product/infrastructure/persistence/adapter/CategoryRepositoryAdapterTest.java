package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.CategoryName;
import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.commerce.product.infrastructure.persistence.repository.CategoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryRepositoryAdapterTest {
    
    @Mock
    private CategoryJpaRepository categoryJpaRepository;
    
    @InjectMocks
    private CategoryRepositoryAdapter categoryRepositoryAdapter;
    
    private CategoryId categoryId;
    private CategoryName categoryName;
    private Category category;
    private CategoryJpaEntity categoryEntity;
    
    @BeforeEach
    void setUp() {
        categoryId = new CategoryId(UUID.randomUUID().toString());
        categoryName = new CategoryName("전자제품");
        category = Category.createRoot(categoryId, categoryName, 1);
        
        categoryEntity = CategoryJpaEntity.builder()
                .id(categoryId.value())
                .name(categoryName.value())
                .parentId(null)
                .level(1)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
    }
    
    @Test
    void save_루트카테고리_성공() {
        // given
        when(categoryJpaRepository.save(any(CategoryJpaEntity.class)))
                .thenReturn(categoryEntity);
        
        // when
        Category savedCategory = categoryRepositoryAdapter.save(category);
        
        // then
        assertThat(savedCategory.getId()).isEqualTo(categoryId);
        assertThat(savedCategory.getName()).isEqualTo(categoryName);
        assertThat(savedCategory.getLevel()).isEqualTo(1);
        
        verify(categoryJpaRepository, times(1)).save(any(CategoryJpaEntity.class));
    }
    
    @Test
    void save_자식카테고리포함_성공() {
        // given
        CategoryId childId = new CategoryId(UUID.randomUUID().toString());
        CategoryName childName = new CategoryName("스마트폰");
        Category childCategory = Category.createChild(childId, childName, categoryId, 2, 1);
        category.addChild(childCategory);
        
        CategoryJpaEntity childEntity = CategoryJpaEntity.builder()
                .id(childId.value())
                .name(childName.value())
                .parentId(categoryId.value())
                .level(2)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
        
        when(categoryJpaRepository.save(any(CategoryJpaEntity.class)))
                .thenReturn(categoryEntity)
                .thenReturn(childEntity);
        
        // when
        Category savedCategory = categoryRepositoryAdapter.save(category);
        
        // then
        assertThat(savedCategory.getId()).isEqualTo(categoryId);
        verify(categoryJpaRepository, times(2)).save(any(CategoryJpaEntity.class));
    }
    
    @Test
    void findById_존재하는경우_성공() {
        // given
        when(categoryJpaRepository.findByIdWithChildren(categoryId.value()))
                .thenReturn(Optional.of(categoryEntity));
        
        // when
        Optional<Category> found = categoryRepositoryAdapter.findById(categoryId);
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(categoryId);
        assertThat(found.get().getName()).isEqualTo(categoryName);
    }
    
    @Test
    void findById_존재하지않는경우_빈Optional반환() {
        // given
        when(categoryJpaRepository.findByIdWithChildren(anyString()))
                .thenReturn(Optional.empty());
        
        // when
        Optional<Category> found = categoryRepositoryAdapter.findById(categoryId);
        
        // then
        assertThat(found).isEmpty();
    }
    
    @Test
    void findRootCategories_성공() {
        // given
        CategoryJpaEntity rootEntity2 = CategoryJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("의류")
                .parentId(null)
                .level(1)
                .sortOrder(2)
                .isActive(true)
                .version(0L)
                .build();
        
        when(categoryJpaRepository.findRootCategories())
                .thenReturn(Arrays.asList(categoryEntity, rootEntity2));
        
        // when
        List<Category> rootCategories = categoryRepositoryAdapter.findRootCategories();
        
        // then
        assertThat(rootCategories).hasSize(2);
        assertThat(rootCategories.get(0).getName().value()).isEqualTo("전자제품");
        assertThat(rootCategories.get(1).getName().value()).isEqualTo("의류");
    }
    
    @Test
    void findByParentId_성공() {
        // given
        CategoryId parentId = new CategoryId(UUID.randomUUID().toString());
        
        CategoryJpaEntity childEntity1 = CategoryJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("스마트폰")
                .parentId(parentId.value())
                .level(2)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
                
        CategoryJpaEntity childEntity2 = CategoryJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("노트북")
                .parentId(parentId.value())
                .level(2)
                .sortOrder(2)
                .isActive(true)
                .version(0L)
                .build();
        
        when(categoryJpaRepository.findByParentId(parentId.value()))
                .thenReturn(Arrays.asList(childEntity1, childEntity2));
        
        // when
        List<Category> children = categoryRepositoryAdapter.findByParentId(parentId);
        
        // then
        assertThat(children).hasSize(2);
        assertThat(children.get(0).getName().value()).isEqualTo("스마트폰");
        assertThat(children.get(1).getName().value()).isEqualTo("노트북");
    }
    
    @Test
    void findActiveCategories_성공() {
        // given
        CategoryJpaEntity activeEntity1 = CategoryJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("활성카테고리1")
                .parentId(null)
                .level(1)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
                
        CategoryJpaEntity activeEntity2 = CategoryJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("활성카테고리2")
                .parentId(null)
                .level(1)
                .sortOrder(2)
                .isActive(true)
                .version(0L)
                .build();
        
        when(categoryJpaRepository.findActiveCategories())
                .thenReturn(Arrays.asList(activeEntity1, activeEntity2));
        
        // when
        List<Category> activeCategories = categoryRepositoryAdapter.findActiveCategories();
        
        // then
        assertThat(activeCategories).hasSize(2);
        assertThat(activeCategories).allMatch(Category::isActive);
    }
    
    @Test
    void findCategoryPath_성공() {
        // given
        CategoryId leafId = new CategoryId(UUID.randomUUID().toString());
        
        CategoryJpaEntity rootEntity = CategoryJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("전자제품")
                .parentId(null)
                .level(1)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
                
        CategoryJpaEntity middleEntity = CategoryJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("모바일기기")
                .parentId(rootEntity.getId())
                .level(2)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
                
        CategoryJpaEntity leafEntity = CategoryJpaEntity.builder()
                .id(leafId.value())
                .name("스마트폰")
                .parentId(middleEntity.getId())
                .level(3)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
        
        when(categoryJpaRepository.findCategoryPath(leafId.value()))
                .thenReturn(Arrays.asList(rootEntity, middleEntity, leafEntity));
        
        // when
        List<Category> path = categoryRepositoryAdapter.findCategoryPath(leafId);
        
        // then
        assertThat(path).hasSize(3);
        assertThat(path.get(0).getName().value()).isEqualTo("전자제품");
        assertThat(path.get(1).getName().value()).isEqualTo("모바일기기");
        assertThat(path.get(2).getName().value()).isEqualTo("스마트폰");
    }
    
    @Test
    void delete_성공() {
        // given
        when(categoryJpaRepository.findById(categoryId.value()))
                .thenReturn(Optional.of(categoryEntity));
        
        // when
        categoryRepositoryAdapter.delete(category);
        
        // then
        verify(categoryJpaRepository).findById(categoryId.value());
        verify(categoryJpaRepository).save(any(CategoryJpaEntity.class));
    }
    
    @Test
    void hasActiveProducts_true반환() {
        // given
        when(categoryJpaRepository.hasActiveProducts(categoryId.value()))
                .thenReturn(true);
        
        // when
        boolean hasActiveProducts = categoryRepositoryAdapter.hasActiveProducts(categoryId);
        
        // then
        assertThat(hasActiveProducts).isTrue();
    }
    
    @Test
    void hasActiveProducts_false반환() {
        // given
        when(categoryJpaRepository.hasActiveProducts(categoryId.value()))
                .thenReturn(false);
        
        // when
        boolean hasActiveProducts = categoryRepositoryAdapter.hasActiveProducts(categoryId);
        
        // then
        assertThat(hasActiveProducts).isFalse();
    }
    
    @Test
    void findAll_성공() {
        // given
        when(categoryJpaRepository.findAllWithHierarchy())
                .thenReturn(Arrays.asList(categoryEntity));
        
        // when
        List<Category> allCategories = categoryRepositoryAdapter.findAll();
        
        // then
        assertThat(allCategories).hasSize(1);
        assertThat(allCategories.get(0).getId()).isEqualTo(categoryId);
    }
    
    @Test
    void findAllById_성공() {
        // given
        CategoryId id1 = new CategoryId(UUID.randomUUID().toString());
        CategoryId id2 = new CategoryId(UUID.randomUUID().toString());
        List<CategoryId> categoryIds = Arrays.asList(id1, id2);
        
        CategoryJpaEntity entity1 = CategoryJpaEntity.builder()
                .id(id1.value())
                .name("카테고리1")
                .parentId(null)
                .level(1)
                .sortOrder(1)
                .isActive(true)
                .version(0L)
                .build();
                
        CategoryJpaEntity entity2 = CategoryJpaEntity.builder()
                .id(id2.value())
                .name("카테고리2")
                .parentId(null)
                .level(1)
                .sortOrder(2)
                .isActive(true)
                .version(0L)
                .build();
        
        when(categoryJpaRepository.findAllByIdIn(anyList()))
                .thenReturn(Arrays.asList(entity1, entity2));
        
        // when
        List<Category> categories = categoryRepositoryAdapter.findAllById(categoryIds);
        
        // then
        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(c -> c.getId().value())
                .containsExactlyInAnyOrder(id1.value(), id2.value());
    }
    
    @Test
    void findAllById_빈리스트시_빈결과반환() {
        // when
        List<Category> categories = categoryRepositoryAdapter.findAllById(List.of());
        
        // then
        assertThat(categories).isEmpty();
        verify(categoryJpaRepository, never()).findAllByIdIn(anyList());
    }
    
    @Test
    void count_성공() {
        // given
        when(categoryJpaRepository.count()).thenReturn(5L);
        
        // when
        long count = categoryRepositoryAdapter.count();
        
        // then
        assertThat(count).isEqualTo(5);
    }
    
    @Test
    void existsById_true반환() {
        // given
        when(categoryJpaRepository.existsById(categoryId.value()))
                .thenReturn(true);
        
        // when
        boolean exists = categoryRepositoryAdapter.existsById(categoryId);
        
        // then
        assertThat(exists).isTrue();
    }
    
    @Test
    void existsById_false반환() {
        // given
        when(categoryJpaRepository.existsById(categoryId.value()))
                .thenReturn(false);
        
        // when
        boolean exists = categoryRepositoryAdapter.existsById(categoryId);
        
        // then
        assertThat(exists).isFalse();
    }
}