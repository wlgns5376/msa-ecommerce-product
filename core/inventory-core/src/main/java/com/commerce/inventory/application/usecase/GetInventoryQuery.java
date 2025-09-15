package com.commerce.inventory.application.usecase;

import jakarta.validation.constraints.NotBlank;

public record GetInventoryQuery(@NotBlank(message = "SKU ID는 필수입니다") String skuId) {
}