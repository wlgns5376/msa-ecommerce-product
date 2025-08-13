package com.commerce.inventory.application.port.in;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceiveStockCommand {
    private final String skuId;
    private final int quantity;
    private final String reference;
}