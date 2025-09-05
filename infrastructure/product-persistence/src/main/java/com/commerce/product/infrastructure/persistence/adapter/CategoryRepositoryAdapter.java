package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.product.domain.model.Category;
import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.repository.CategoryRepository;
import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.commerce.product.infrastructure.persistence.repository.CategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Category 도메인 리포지토리의 JPA 구현체
 * 카테고리 정보의 영속성을 관리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {
    
    private final CategoryJpaRepository categoryJpaRepository;
    
    @Override
    @Transactional
    public Category save(Category category) {
        CategoryJpaEntity entity = CategoryJpaEntity.fromDomainModel(category);
        CategoryJpaEntity savedEntity = categoryJpaRepository.save(entity);
        
        // 자식 카테고리들도 함께 저장
        if (!category.getChildren().isEmpty()) {
            for (Category child : category.getChildren()) {
                save(child);
            }
        }
        
        return savedEntity.toDomainModel();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(CategoryId id) {
        return categoryJpaRepository.findByIdWithChildren(id.value())
                .map(CategoryJpaEntity::toDomainModelWithChildren);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Category> findRootCategories() {
        return categoryJpaRepository.findRootCategories().stream()
                .map(CategoryJpaEntity::toDomainModelWithChildren)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Category> findByParentId(CategoryId parentId) {
        return categoryJpaRepository.findByParentId(parentId.value()).stream()
                .map(CategoryJpaEntity::toDomainModelWithChildren)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Category> findActiveCategories() {
        return categoryJpaRepository.findActiveCategories().stream()
                .map(CategoryJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Category> findCategoryPath(CategoryId leafCategoryId) {
        return categoryJpaRepository.findCategoryPath(leafCategoryId.value()).stream()
                .map(CategoryJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void delete(Category category) {
        categoryJpaRepository.findById(category.getId().value())
                .ifPresent(entity -> {
                    entity.markAsDeleted();
                    categoryJpaRepository.save(entity);
                    
                    // 자식 카테고리들도 함께 삭제 (soft delete)
                    if (!category.getChildren().isEmpty()) {
                        for (Category child : category.getChildren()) {
                            delete(child);
                        }
                    }
                });
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveProducts(CategoryId categoryId) {
        return categoryJpaRepository.hasActiveProducts(categoryId.value());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        // 전체 카테고리를 계층 구조로 조회
        List<CategoryJpaEntity> rootEntities = categoryJpaRepository.findAllWithHierarchy();
        return rootEntities.stream()
                .map(CategoryJpaEntity::toDomainModelWithChildren)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllById(List<CategoryId> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        
        List<String> ids = categoryIds.stream()
                .map(CategoryId::value)
                .distinct()
                .collect(Collectors.toList());
                
        return categoryJpaRepository.findAllByIdIn(ids).stream()
                .map(CategoryJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public long count() {
        return categoryJpaRepository.count();
    }
    
    @Override
    @Transactional(readOnly = true) 
    public boolean existsById(CategoryId id) {
        return categoryJpaRepository.existsById(id.value());
    }
    
    @Override
    @Transactional
    public void deleteById(CategoryId id) {
        categoryJpaRepository.findById(id.value())
                .ifPresent(entity -> {
                    entity.markAsDeleted();
                    categoryJpaRepository.save(entity);
                });
    }
    
    @Override
    @Transactional
    public List<Category> saveAll(List<Category> entities) {
        return entities.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }
}