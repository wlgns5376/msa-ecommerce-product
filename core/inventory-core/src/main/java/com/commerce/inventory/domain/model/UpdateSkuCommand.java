package com.commerce.inventory.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateSkuCommand {
    private final String name;
    private final String description;
    private final Weight weight;
    private final Volume volume;
}