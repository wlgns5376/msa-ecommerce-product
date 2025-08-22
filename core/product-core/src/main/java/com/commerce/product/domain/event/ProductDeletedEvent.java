package com.commerce.product.domain.event;


import com.commerce.product.domain.model.ProductId;
import lombok.Getter;

@Getter
public class ProductDeletedEvent extends AbstractDomainEvent {
    private final ProductId productId;

    public ProductDeletedEvent(ProductId productId) {
        super();
        this.productId = productId;
    }

    @Override
    public String getAggregateId() {
        return productId.value();
    }

    @Override
    public String getEventType() {
        return "product.deleted";
    }
}