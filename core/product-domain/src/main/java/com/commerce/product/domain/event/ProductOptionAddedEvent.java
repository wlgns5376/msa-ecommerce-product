package com.commerce.product.domain.event;


import com.commerce.product.domain.model.ProductId;
import com.commerce.product.domain.model.ProductOption;
import lombok.Getter;

@Getter
public class ProductOptionAddedEvent extends AbstractDomainEvent {
    private final ProductId productId;
    private final ProductOption option;

    public ProductOptionAddedEvent(ProductId productId, ProductOption option) {
        super();
        this.productId = productId;
        this.option = option;
    }

    @Override
    public String getAggregateId() {
        return productId.getValue();
    }

    @Override
    public String getEventType() {
        return "product.option.added";
    }
}