package com.commerce.inventory.application.port.in;

import com.commerce.common.application.usecase.UseCase;

/**
 * SKU 생성 유스케이스
 * 
 * <p>새로운 SKU(Stock Keeping Unit)를 생성합니다.</p>
 */
public interface CreateSkuUseCase extends UseCase<CreateSkuCommand, CreateSkuResponse> {
}