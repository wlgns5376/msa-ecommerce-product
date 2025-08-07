package com.commerce.product.domain.model;

import com.commerce.product.domain.event.*;
import com.commerce.product.domain.exception.*;
import lombok.Getter;

import java.util.ArrayList;
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

    private Product(ProductId id, ProductName name, String description, ProductType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = ProductStatus.DRAFT;
        this.options = new ArrayList<>();
        this.categoryIds = new ArrayList<>();
        this.outOfStock = false;
    }

    public static Product create(ProductName name, String description, ProductType type) {
        validateCreation(name, type);
        
        ProductId id = ProductId.generate();
        Product product = new Product(id, name, description, type);
        product.addDomainEvent(new ProductCreatedEvent(id, name.getValue(), type));
        
        return product;
    }

    private static void validateCreation(ProductName name, ProductType type) {
        if (name == null) {
            throw new InvalidProductException("Product name is required");
        }
        if (type == null) {
            throw new InvalidProductException("Product type is required");
        }
    }

    public void addOption(ProductOption option) {
        validateOptionAddition(option);
        
        options.add(option);
        addDomainEvent(new ProductOptionAddedEvent(id, option));
    }

    private void validateOptionAddition(ProductOption option) {
        if (status == ProductStatus.DELETED) {
            throw new InvalidProductException("Cannot add option to deleted product");
        }
        
        if (type == ProductType.BUNDLE && !option.hasMultipleSkus()) {
            throw new InvalidOptionException("Bundle product must have options with multiple SKUs");
        }
        
        if (options.contains(option)) {
            throw new DuplicateOptionException("Option already exists");
        }
    }

    public void update(ProductName name, String description) {
        if (status == ProductStatus.DELETED) {
            throw new InvalidProductException("Cannot update deleted product");
        }
        
        this.name = name;
        this.description = description;
        addDomainEvent(new ProductUpdatedEvent(id, name.getValue(), description));
    }

    public void activate() {
        if (options.isEmpty()) {
            throw new InvalidProductException("Product must have at least one option to be activated");
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
            throw new MaxCategoryLimitException("Product can be assigned to maximum 5 categories");
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
}