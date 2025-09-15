package com.commerce.product.infrastructure.persistence.adapter;

import com.commerce.product.domain.repository.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 범용 레포지토리 어댑터
 * JPA Repository를 도메인 Repository로 변환합니다.
 * 
 * @param <DOMAIN> 도메인 엔티티 타입
 * @param <ENTITY> JPA 엔티티 타입
 * @param <ID> 식별자 타입
 */
public abstract class GenericRepositoryAdapter<DOMAIN, ENTITY, ID> implements Repository<DOMAIN, ID> {
    
    protected final JpaRepository<ENTITY, ID> jpaRepository;
    protected final Function<ENTITY, DOMAIN> toDomainMapper;
    protected final Function<DOMAIN, ENTITY> toEntityMapper;
    
    protected GenericRepositoryAdapter(
            JpaRepository<ENTITY, ID> jpaRepository,
            Function<ENTITY, DOMAIN> toDomainMapper,
            Function<DOMAIN, ENTITY> toEntityMapper) {
        this.jpaRepository = jpaRepository;
        this.toDomainMapper = toDomainMapper;
        this.toEntityMapper = toEntityMapper;
    }
    
    @Override
    public DOMAIN save(DOMAIN domain) {
        ENTITY entity = toEntityMapper.apply(domain);
        ENTITY savedEntity = jpaRepository.save(entity);
        return toDomainMapper.apply(savedEntity);
    }
    
    @Override
    public Optional<DOMAIN> findById(ID id) {
        return jpaRepository.findById(id)
                .map(toDomainMapper);
    }
    
    @Override
    public List<DOMAIN> findAll() {
        return jpaRepository.findAll().stream()
                .map(toDomainMapper)
                .toList();
    }
    
    @Override
    public void delete(DOMAIN domain) {
        ENTITY entity = toEntityMapper.apply(domain);
        jpaRepository.delete(entity);
    }
    
    @Override
    public void deleteById(ID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsById(ID id) {
        return jpaRepository.existsById(id);
    }
}