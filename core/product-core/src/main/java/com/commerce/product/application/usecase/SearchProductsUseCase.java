package com.commerce.product.application.usecase;

import com.commerce.common.application.usecase.UseCase;

/**
 * 상품 검색 유스케이스
 * 카테고리, 이름, 가격 범위 등 다양한 조건으로 상품을 검색합니다.
 */
public interface SearchProductsUseCase extends UseCase<SearchProductsRequest, SearchProductsResponse> {
}