package com.commerce.product.domain.event;

import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.model.ProductType;
import lombok.Getter;

@Getter
public class ProductCreatedEvent extends AbstractDomainEvent {
    private final ProductId productId;
    private final String name;
    private final ProductType type;

    public ProductCreatedEvent(ProductId productId, String name, ProductType type) {
        super();
        this.productId = productId;
        this.name = name;
        this.type = type;
    }

    @Override
    public String getAggregateId() {
        return productId.getValue();
    }

    @Override
    public String getEventType() {
        return "product.created";
    }
}