package com.commerce.product.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 도메인 엔티티의 기본 클래스
 */
@Getter
public abstract class BaseEntity<ID> {
    
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    protected BaseEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    protected BaseEntity(LocalDateTime createdAt) {
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    /**
     * 엔티티가 업데이트되었을 때 호출합니다.
     */
    protected void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 엔티티의 ID를 반환합니다.
     */
    public abstract ID getId();
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BaseEntity<?> that = (BaseEntity<?>) obj;
        return Objects.equals(getId(), that.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}