package com.commerce.product.domain.event;


import com.commerce.product.domain.model.ProductId;
import lombok.Getter;

@Getter
public class ProductActivatedEvent extends AbstractDomainEvent {
    private final ProductId productId;

    public ProductActivatedEvent(ProductId productId) {
        super();
        this.productId = productId;
    }

    @Override
    public String getAggregateId() {
        return productId.value();
    }

    @Override
    public String getEventType() {
        return "product.activated";
    }
}