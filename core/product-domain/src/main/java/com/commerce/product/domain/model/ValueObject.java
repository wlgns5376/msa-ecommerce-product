package com.commerce.product.domain.model;

/**
 * 값 객체의 마커 인터페이스
 * 모든 값 객체는 이 인터페이스를 구현해야 합니다.
 * 
 * 값 객체의 특징:
 * 1. 불변성 (Immutable)
 * 2. 동등성은 속성 기반 (equals/hashCode 구현 필수)
 * 3. 식별자가 없음
 */
public interface ValueObject {
    
    // 마커 인터페이스로 별도 메서드 정의 없음
    // 구현 클래스에서 equals, hashCode를 반드시 구현해야 함
}