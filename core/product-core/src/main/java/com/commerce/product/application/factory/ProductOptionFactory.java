package com.commerce.product.application.factory;

import com.commerce.product.application.usecase.AddProductOptionRequest;
import com.commerce.product.domain.exception.InvalidProductOptionException;
import com.commerce.product.domain.model.Currency;
import com.commerce.product.domain.model.Money;
import com.commerce.product.domain.model.ProductOption;
import com.commerce.product.domain.model.SkuMapping;
import org.springframework.stereotype.Component;

@Component
public class ProductOptionFactory {
    
    public ProductOption create(AddProductOptionRequest request) {
        Currency currency;
        try {
            currency = Currency.valueOf(request.getCurrency().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidProductOptionException("유효하지 않은 통화입니다: " + request.getCurrency());
        }
        Money price = new Money(request.getPrice(), currency);
        SkuMapping skuMapping = SkuMapping.of(request.getSkuMappings());

        if (skuMapping.isBundle()) {
            return ProductOption.bundle(request.getOptionName(), price, skuMapping);
        } else {
            return ProductOption.single(request.getOptionName(), price, skuMapping);
        }
    }
}