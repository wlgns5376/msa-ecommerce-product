package com.commerce.inventory.application.usecase;

public interface GetSkuByIdUseCase {
    GetSkuByIdResponse execute(GetSkuByIdQuery query);
}