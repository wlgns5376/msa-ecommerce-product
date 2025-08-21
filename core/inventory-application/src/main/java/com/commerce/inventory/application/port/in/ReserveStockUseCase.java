package com.commerce.inventory.application.port.in;

import com.commerce.common.application.usecase.UseCase;

/**
 * 재고 예약 유스케이스
 * 
 * <p>지정된 SKU들의 재고를 예약합니다.</p>
 */
public interface ReserveStockUseCase extends UseCase<ReserveStockCommand, ReserveStockResponse> {
}