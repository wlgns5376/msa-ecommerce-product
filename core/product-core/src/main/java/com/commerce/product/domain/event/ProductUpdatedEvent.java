package com.commerce.product.domain.event;


import com.commerce.product.domain.model.ProductId;
import lombok.Getter;

@Getter
public class ProductUpdatedEvent extends AbstractDomainEvent {
    private final ProductId productId;
    private final String name;
    private final String description;

    public ProductUpdatedEvent(ProductId productId, String name, String description) {
        super();
        this.productId = productId;
        this.name = name;
        this.description = description;
    }

    @Override
    public String getAggregateId() {
        return productId.value();
    }

    @Override
    public String getEventType() {
        return "product.updated";
    }
}