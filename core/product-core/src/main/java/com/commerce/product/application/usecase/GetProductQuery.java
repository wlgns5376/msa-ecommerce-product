package com.commerce.product.application.usecase;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetProductQuery {
    private final String productId;
}