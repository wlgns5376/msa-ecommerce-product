package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.ProductSearchCriteria;
import com.commerce.product.infrastructure.persistence.dto.ProductSearchResultDto;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.commerce.product.infrastructure.persistence.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {
    
    private final ProductJpaRepository productJpaRepository;
    
    @Override
    @Transactional
    public Product save(Product product) {
        ProductJpaEntity entity = ProductJpaEntity.fromDomainModel(product);
        ProductJpaEntity savedEntity = productJpaRepository.save(entity);
        return savedEntity.toDomainModel();
    }
    
    @Override
    @Transactional
    public List<Product> saveAll(List<Product> products) {
        List<ProductJpaEntity> entities = products.stream()
                .map(ProductJpaEntity::fromDomainModel)
                .collect(Collectors.toList());
        List<ProductJpaEntity> savedEntities = productJpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(ProductJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(ProductId id) {
        return productJpaRepository.findByIdWithDetails(id.value())
                .map(ProductJpaEntity::toDomainModel);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findByIdWithoutAssociations(ProductId id) {
        return productJpaRepository.findById(id.value())
                .map(ProductJpaEntity::toDomainModel);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productJpaRepository.findAll().stream()
                .map(ProductJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void delete(Product product) {
        productJpaRepository.findById(product.getId().value())
                .ifPresent(entity -> {
                    entity.markAsDeleted();
                    productJpaRepository.save(entity);
                });
    }
    
    @Override
    @Transactional
    public void deleteById(ProductId id) {
        productJpaRepository.findById(id.value())
                .ifPresent(entity -> {
                    entity.markAsDeleted();
                    productJpaRepository.save(entity);
                });
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsById(ProductId id) {
        return productJpaRepository.existsById(id.value());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Product> findByCategory(CategoryId categoryId, int offset, int limit) {
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);
        Page<ProductJpaEntity> entityPage = productJpaRepository.findByCategoryId(categoryId.value(), pageable);
        return entityPage.getContent().stream()
                .map(ProductJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ProductOption> findOptionById(String optionId) {
        // 이 메서드는 별도의 옵션 조회 쿼리가 필요할 수 있습니다.
        // 현재는 구현하지 않고 빈 Optional을 반환합니다.
        return Optional.empty();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Product> findActiveProducts(int offset, int limit) {
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);
        Page<ProductJpaEntity> entityPage = productJpaRepository.findActiveProducts(pageable);
        return entityPage.getContent().stream()
                .map(ProductJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Product> searchByName(String keyword, int offset, int limit) {
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);
        Page<ProductJpaEntity> entityPage = productJpaRepository.searchByName(keyword, pageable);
        return entityPage.getContent().stream()
                .map(ProductJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        return productJpaRepository.count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        // 참고: SearchProductsRequest의 build() 메서드에서 statuses가 null이거나 빈 경우를 
        // 항상 DEFAULT_STATUSES로 설정하므로, criteria.getStatuses()는 절대 빈 Set이 될 수 없음
        
        Page<ProductJpaEntity> entityPage = productJpaRepository.searchProducts(
            criteria.getCategoryId(),
            criteria.getKeyword(),
            criteria.getMinPrice(),
            criteria.getMaxPrice(),
            criteria.getStatuses(),
            pageable
        );
        
        return entityPage.map(ProductJpaEntity::toDomainModel);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductSearchResult> searchProductsOptimized(ProductSearchCriteria criteria, Pageable pageable) {
        Page<ProductSearchResultDto> dtoPage = productJpaRepository.searchProductsWithDto(
            criteria.getCategoryId(),
            criteria.getKeyword(),
            criteria.getMinPrice(),
            criteria.getMaxPrice(),
            criteria.getStatuses(),
            pageable
        );
        
        return dtoPage.map(dto -> new ProductSearchResult(
            new ProductId(dto.getId()),
            new ProductName(dto.getName()),
            dto.getDescription(),
            dto.getType(),
            dto.getStatus(),
            dto.getMinPrice(),
            dto.getMaxPrice(),
            dto.getCategoryIds().stream()
                .map(CategoryId::new)
                .toList(),
            dto.getCreatedAt()
        ));
    }
}