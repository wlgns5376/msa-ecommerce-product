package com.commerce.inventory.application.port.in;

import jakarta.validation.constraints.NotBlank;

public record GetInventoryQuery(@NotBlank(message = "SKU ID is required") String skuId) {
}
