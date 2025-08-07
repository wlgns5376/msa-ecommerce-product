package com.commerce.product.domain.model;

import com.commerce.product.domain.event.CategoryActivatedEvent;
import com.commerce.product.domain.event.CategoryDeactivatedEvent;
import com.commerce.product.domain.exception.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Category extends AggregateRoot<CategoryId> {
    private static final int MAX_LEVEL = 3;
    
    private final CategoryId id;
    private CategoryName name;
    private final CategoryId parentId;
    private final int level;
    private int sortOrder;
    private boolean isActive;
    private final List<Category> children;
    
    @Setter
    private Category parent;
    
    @Setter
    private boolean hasActiveProducts;

    private Category(CategoryId id, CategoryName name, CategoryId parentId, int level, int sortOrder) {
        validateLevel(level);
        
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.level = level;
        this.sortOrder = sortOrder;
        this.isActive = true;
        this.children = new ArrayList<>();
        this.hasActiveProducts = false;
    }

    public static Category createRoot(CategoryId id, CategoryName name, int sortOrder) {
        return new Category(id, name, null, 1, sortOrder);
    }

    public static Category createChild(CategoryId id, CategoryName name, CategoryId parentId, int level, int sortOrder) {
        return new Category(id, name, parentId, level, sortOrder);
    }

    private static void validateLevel(int level) {
        if (level > MAX_LEVEL) {
            throw new InvalidCategoryLevelException("Maximum category level is " + MAX_LEVEL);
        }
    }

    public void addChild(Category child) {
        if (level >= MAX_LEVEL) {
            throw new MaxCategoryDepthException("Cannot add child to level " + level + " category");
        }
        
        child.setParent(this);
        children.add(child);
    }

    public void activate() {
        this.isActive = true;
        addDomainEvent(new CategoryActivatedEvent(id));
    }

    public void deactivate() {
        if (hasActiveProducts) {
            throw new CannotDeactivateCategoryException("Cannot deactivate category with active products");
        }
        
        this.isActive = false;
        addDomainEvent(new CategoryDeactivatedEvent(id));
    }

    public void updateName(CategoryName name) {
        this.name = name;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setParent(Category parent) {
        if (this.equals(parent)) {
            throw new InvalidCategoryHierarchyException("Category cannot be its own parent");
        }
        this.parent = parent;
    }

    public String getFullPath() {
        if (parent == null) {
            return name.getValue();
        }
        return parent.getFullPath() + " > " + name.getValue();
    }
}