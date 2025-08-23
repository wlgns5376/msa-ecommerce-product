package com.commerce.product.domain.model;

import com.commerce.product.domain.event.*;
import com.commerce.product.domain.exception.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Product extends AggregateRoot<ProductId> {
    private final ProductId id;
    private ProductName name;
    private String description;
    private final ProductType type;
    private ProductStatus status;
    private final List<ProductOption> options;
    private final List<CategoryId> categoryIds;
    private boolean outOfStock;
    private Long version;

    public Product(ProductId id, ProductName name, String description, ProductType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = ProductStatus.DRAFT;
        this.options = new ArrayList<>();
        this.categoryIds = new ArrayList<>();
        this.outOfStock = false;
        this.version = 0L;
    }
    
    private Product(ProductId id, ProductName name, String description, ProductType type,
                   ProductStatus status, List<ProductOption> options, List<CategoryId> categoryIds,
                   boolean outOfStock, Long version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status;
        this.options = new ArrayList<>(options);
        this.categoryIds = new ArrayList<>(categoryIds);
        this.outOfStock = outOfStock;
        this.version = version;
    }

    public static Product create(ProductName name, String description, ProductType type) {
        validateCreation(name, type);
        
        String validDescription = description != null ? description : "";
        
        ProductId id = ProductId.generate();
        Product product = new Product(id, name, validDescription, type);
        product.addDomainEvent(new ProductCreatedEvent(id, name.value(), type));
        
        return product;
    }

    private static void validateCreation(ProductName name, ProductType type) {
        if (name == null) {
            throw new InvalidProductException("상품명은 필수입니다");
        }
        if (type == null) {
            throw new InvalidProductException("상품 타입은 필수입니다");
        }
    }

    public void addOption(ProductOption option) {
        validateOptionAddition(option);
        
        options.add(option);
        addDomainEvent(new ProductOptionAddedEvent(id, option));
    }

    private void validateOptionAddition(ProductOption option) {
        if (status == ProductStatus.DELETED) {
            throw new InvalidProductException("삭제된 상품에는 옵션을 추가할 수 없습니다");
        }
        
        if (type == ProductType.BUNDLE && !option.isBundle()) {
            throw new InvalidOptionException("번들 상품은 번들 옵션만 가질 수 있습니다");
        }
        
        if (type == ProductType.NORMAL && option.isBundle()) {
            throw new InvalidOptionException("일반 상품은 번들 옵션을 가질 수 없습니다");
        }
        
        if (options.stream().anyMatch(o -> o.getName().equals(option.getName()))) {
            throw new DuplicateOptionException("동일한 이름의 옵션이 이미 존재합니다");
        }
    }

    public boolean update(String name, String description) {
        if (status == ProductStatus.DELETED) {
            throw new InvalidProductException("삭제된 상품은 수정할 수 없습니다");
        }
        
        boolean changed = false;
        if (name != null && !this.name.value().equals(name)) {
            this.name = new ProductName(name);
            changed = true;
        }
        if (description != null && !this.description.equals(description)) {
            this.description = description;
            changed = true;
        }
        
        if (changed) {
            addDomainEvent(new ProductUpdatedEvent(id, this.name.value(), this.description));
        }
        
        return changed;
    }

    public void activate() {
        if (options.isEmpty()) {
            throw new InvalidProductException("상품을 활성화하려면 최소 하나의 옵션이 필요합니다");
        }
        
        this.status = ProductStatus.ACTIVE;
        addDomainEvent(new ProductActivatedEvent(id));
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
        addDomainEvent(new ProductDeactivatedEvent(id));
    }

    public void delete() {
        this.status = ProductStatus.DELETED;
        addDomainEvent(new ProductDeletedEvent(id));
    }

    public void assignCategories(List<CategoryId> categoryIds) {
        if (categoryIds.size() > 5) {
            throw new MaxCategoryLimitException("상품은 최대 5개의 카테고리에만 할당할 수 있습니다");
        }
        
        this.categoryIds.clear();
        this.categoryIds.addAll(categoryIds);
    }

    public void markAsOutOfStock() {
        this.outOfStock = true;
        addDomainEvent(new ProductOutOfStockEvent(id));
    }

    public void markAsInStock() {
        this.outOfStock = false;
        addDomainEvent(new ProductInStockEvent(id));
    }

    public List<ProductOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public List<CategoryId> getCategoryIds() {
        return Collections.unmodifiableList(categoryIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    public static Product restore(ProductId id, ProductName name, String description, ProductType type,
                                  ProductStatus status, List<ProductOption> options, List<CategoryId> categoryIds,
                                  boolean outOfStock, Long version) {
        return new Product(id, name, description, type, status, options, categoryIds, outOfStock, version);
    }
}