package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.product.domain.model.CategoryId;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.model.ProductOption;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.commerce.product.infrastructure.persistence.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    public List<Product> findAll() {
        return productJpaRepository.findAll().stream()
                .filter(entity -> entity.getDeletedAt() == null)
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
        List<ProductJpaEntity> entities = productJpaRepository.findByCategoryId(categoryId.value());
        return entities.stream()
                .skip(offset)
                .limit(limit)
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
        List<ProductJpaEntity> entities = productJpaRepository.findActiveProducts();
        return entities.stream()
                .skip(offset)
                .limit(limit)
                .map(ProductJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Product> searchByName(String keyword, int offset, int limit) {
        List<ProductJpaEntity> entities = productJpaRepository.searchByName(keyword);
        return entities.stream()
                .skip(offset)
                .limit(limit)
                .map(ProductJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        return productJpaRepository.count();
    }
}