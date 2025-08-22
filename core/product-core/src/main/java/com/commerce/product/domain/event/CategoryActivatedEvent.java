package com.commerce.product.domain.event;


import com.commerce.product.domain.model.CategoryId;
import lombok.Getter;

@Getter
public class CategoryActivatedEvent extends AbstractDomainEvent {
    private final CategoryId categoryId;

    public CategoryActivatedEvent(CategoryId categoryId) {
        super();
        this.categoryId = categoryId;
    }

    @Override
    public String getAggregateId() {
        return categoryId.value();
    }

    @Override
    public String getEventType() {
        return "category.activated";
    }
}