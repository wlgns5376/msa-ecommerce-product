package com.commerce.product.domain.model.inventory;

import com.commerce.common.domain.model.Quantity;

public interface Inventory {
    Quantity getAvailableQuantity();
}