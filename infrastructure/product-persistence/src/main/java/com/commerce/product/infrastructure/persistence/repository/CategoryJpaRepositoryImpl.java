package com.commerce.product.infrastructure.persistence.repository;

import com.commerce.product.domain.model.ProductStatus;
import com.commerce.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.commerce.product.infrastructure.persistence.entity.QCategoryJpaEntity;
import com.commerce.product.infrastructure.persistence.entity.QProductCategoryJpaEntity;
import com.commerce.product.infrastructure.persistence.entity.QProductJpaEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryJpaRepositoryImpl implements CategoryJpaRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public Optional<CategoryJpaEntity> findByIdWithChildrenQueryDsl(String id) {
        QCategoryJpaEntity category = QCategoryJpaEntity.categoryJpaEntity;
        QCategoryJpaEntity children = new QCategoryJpaEntity("children");
        
        CategoryJpaEntity result = queryFactory
                .selectFrom(category)
                .leftJoin(category.children, children).fetchJoin()
                .where(category.id.eq(id)
                        .and(category.deletedAt.isNull()))
                .fetchOne();
                
        return Optional.ofNullable(result);
    }
    
    @Override
    public boolean hasActiveProductsQueryDsl(String categoryId) {
        QProductCategoryJpaEntity productCategory = QProductCategoryJpaEntity.productCategoryJpaEntity;
        QProductJpaEntity product = QProductJpaEntity.productJpaEntity;
        QCategoryJpaEntity category = QCategoryJpaEntity.categoryJpaEntity;
        
        Long count = queryFactory
                .select(productCategory.count())
                .from(productCategory)
                .join(productCategory.product, product)
                .where(productCategory.categoryId.eq(categoryId)
                        .and(product.status.eq(ProductStatus.ACTIVE))
                        .and(product.deletedAt.isNull()))
                .fetchOne();
                
        return count != null && count > 0;
    }
    
    @Override
    public List<CategoryJpaEntity> findAllWithHierarchyQueryDsl() {
        QCategoryJpaEntity category = QCategoryJpaEntity.categoryJpaEntity;
        QCategoryJpaEntity children = new QCategoryJpaEntity("children");
        
        return queryFactory
                .selectDistinct(category)
                .from(category)
                .leftJoin(category.children, children).fetchJoin()
                .where(category.parentId.isNull()
                        .and(category.deletedAt.isNull()))
                .orderBy(category.sortOrder.asc(), category.name.asc())
                .fetch();
    }
    
    @Override
    public List<CategoryJpaEntity> findCategoryPath(String leafCategoryId) {
        List<CategoryJpaEntity> path = new ArrayList<>();
        CategoryJpaEntity current = queryFactory
                .selectFrom(QCategoryJpaEntity.categoryJpaEntity)
                .where(QCategoryJpaEntity.categoryJpaEntity.id.eq(leafCategoryId)
                        .and(QCategoryJpaEntity.categoryJpaEntity.deletedAt.isNull()))
                .fetchOne();
                
        while (current != null) {
            path.add(0, current); // 리스트의 맨 앞에 추가하여 루트->리프 순서로 만듦
            if (current.getParentId() != null) {
                current = queryFactory
                        .selectFrom(QCategoryJpaEntity.categoryJpaEntity)
                        .where(QCategoryJpaEntity.categoryJpaEntity.id.eq(current.getParentId())
                                .and(QCategoryJpaEntity.categoryJpaEntity.deletedAt.isNull()))
                        .fetchOne();
            } else {
                current = null;
            }
        }
        
        return path;
    }
    
    @Override
    public List<CategoryJpaEntity> findRootCategoriesQueryDsl() {
        QCategoryJpaEntity category = QCategoryJpaEntity.categoryJpaEntity;
        
        return queryFactory
                .selectFrom(category)
                .where(category.parentId.isNull()
                        .and(category.deletedAt.isNull()))
                .orderBy(category.sortOrder.asc(), category.name.asc())
                .fetch();
    }
    
    @Override
    public List<CategoryJpaEntity> findByParentIdQueryDsl(String parentId) {
        QCategoryJpaEntity category = QCategoryJpaEntity.categoryJpaEntity;
        
        return queryFactory
                .selectFrom(category)
                .where(category.parentId.eq(parentId)
                        .and(category.deletedAt.isNull()))
                .orderBy(category.sortOrder.asc(), category.name.asc())
                .fetch();
    }
    
    @Override
    public List<CategoryJpaEntity> findActiveCategoriesQueryDsl() {
        QCategoryJpaEntity category = QCategoryJpaEntity.categoryJpaEntity;
        
        return queryFactory
                .selectFrom(category)
                .where(category.isActive.eq(true)
                        .and(category.deletedAt.isNull()))
                .orderBy(category.level.asc(), category.sortOrder.asc(), category.name.asc())
                .fetch();
    }
    
    @Override
    public List<CategoryJpaEntity> findAllByIdInQueryDsl(List<String> categoryIds) {
        QCategoryJpaEntity category = QCategoryJpaEntity.categoryJpaEntity;
        
        return queryFactory
                .selectFrom(category)
                .where(category.id.in(categoryIds)
                        .and(category.deletedAt.isNull()))
                .fetch();
    }
}