package com.commerce.inventory.application.usecase;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CreateSkuResponse {
    private final String id;
    private final String code;
    private final String name;
    private final String description;
    private final Double weight;
    private final String weightUnit;
    private final Double volume;
    private final String volumeUnit;
    private final LocalDateTime createdAt;
}