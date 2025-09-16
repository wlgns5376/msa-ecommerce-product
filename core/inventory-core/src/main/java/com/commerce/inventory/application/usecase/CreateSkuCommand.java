package com.commerce.inventory.application.usecase;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateSkuCommand {
    private final String code;
    private final String name;
    private final String description;
    private final Double weight;
    private final String weightUnit;
    private final Double volume;
    private final String volumeUnit;
}