package com.commerce.inventory.domain.application.usecase;

import com.commerce.common.application.usecase.UseCase;

/**
 * 재고 조회 유스케이스
 * 
 * <p>특정 SKU의 재고 정보를 조회하는 기능을 제공합니다.
 * 재고가 존재하지 않는 경우 모든 수량을 0으로 반환합니다.</p>
 */
public interface GetInventoryUseCase extends UseCase<GetInventoryQuery, InventoryResponse> {
}