package com.commerce.product.domain.repository;

import java.util.List;
import java.util.Optional;

/**
 * 레포지토리의 기본 인터페이스
 * 
 * @param <T> 엔티티 타입
 * @param <ID> 식별자 타입
 */
public interface Repository<T, ID> {
    
    /**
     * 엔티티를 저장합니다.
     */
    T save(T entity);
    
    /**
     * ID로 엔티티를 조회합니다.
     */
    Optional<T> findById(ID id);
    
    /**
     * 모든 엔티티를 조회합니다.
     */
    List<T> findAll();
    
    /**
     * 엔티티를 삭제합니다.
     */
    void delete(T entity);
    
    /**
     * ID로 엔티티를 삭제합니다.
     */
    void deleteById(ID id);
    
    /**
     * ID에 해당하는 엔티티가 존재하는지 확인합니다.
     */
    boolean existsById(ID id);
}