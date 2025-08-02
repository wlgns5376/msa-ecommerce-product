package com.commerce.product.domain.application.usecase;

/**
 * 유스케이스의 기본 인터페이스
 * 
 * @param <REQUEST> 요청 타입
 * @param <RESPONSE> 응답 타입
 */
public interface UseCase<REQUEST, RESPONSE> {
    
    /**
     * 유스케이스를 실행합니다.
     * 
     * @param request 요청 객체
     * @return 응답 객체
     */
    RESPONSE execute(REQUEST request);
}