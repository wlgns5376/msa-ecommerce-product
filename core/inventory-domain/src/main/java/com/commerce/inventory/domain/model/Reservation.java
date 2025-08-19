package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.exception.InvalidReservationException;
import com.commerce.inventory.domain.exception.InvalidReservationStateException;
import com.commerce.product.domain.model.BaseEntity;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Reservation extends BaseEntity<ReservationId> {
    
    private final ReservationId id;
    private final SkuId skuId;
    private final Quantity quantity;
    private final String orderId;
    private final LocalDateTime expiresAt;
    private ReservationStatus status;
    private final LocalDateTime createdAt;
    private Long version;
    
    private Reservation(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt,
            LocalDateTime createdAt
    ) {
        this(id, skuId, quantity, orderId, expiresAt, ReservationStatus.ACTIVE, createdAt, 0L, true);
    }
    
    private Reservation(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt,
            ReservationStatus status,
            LocalDateTime createdAt,
            Long version,
            boolean isNewCreation
    ) {
        if (isNewCreation) {
            validateCreate(id, skuId, quantity, orderId, expiresAt, createdAt);
        } else {
            validateRestore(id, skuId, quantity, orderId, expiresAt, status, createdAt, version);
        }
        
        this.id = id;
        this.skuId = skuId;
        this.quantity = quantity;
        this.orderId = orderId;
        this.expiresAt = expiresAt;
        this.status = status;
        this.createdAt = createdAt;
        this.version = version;
    }
    
    public static Reservation create(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt,
            LocalDateTime currentTime
    ) {
        return new Reservation(id, skuId, quantity, orderId, expiresAt, currentTime);
    }
    
    public static Reservation createWithTTL(
            SkuId skuId,
            Quantity quantity,
            String orderId,
            int ttlSeconds,
            LocalDateTime currentTime
    ) {
        return new Reservation(
                ReservationId.generate(),
                skuId,
                quantity,
                orderId,
                currentTime.plusSeconds(ttlSeconds),
                currentTime
        );
    }
    
    public static Reservation restore(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt,
            ReservationStatus status,
            LocalDateTime createdAt,
            Long version
    ) {
        return new Reservation(id, skuId, quantity, orderId, expiresAt, status, createdAt, version, false);
    }
    
    public boolean isExpired(LocalDateTime currentTime) {
        return currentTime.isAfter(expiresAt);
    }
    
    public boolean isActive(LocalDateTime currentTime) {
        return status == ReservationStatus.ACTIVE && !isExpired(currentTime);
    }
    
    public void release() {
        if (status == ReservationStatus.RELEASED) {
            throw new InvalidReservationStateException("이미 해제된 예약입니다");
        }
        
        if (status == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException("확정된 예약은 해제할 수 없습니다");
        }
        
        this.status = ReservationStatus.RELEASED;
        markAsUpdated();
    }
    
    public void confirm(LocalDateTime currentTime) {
        if (isExpired(currentTime)) {
            throw new InvalidReservationStateException("만료된 예약은 확정할 수 없습니다");
        }
        
        if (status == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException("이미 확정된 예약입니다");
        }
        
        if (status == ReservationStatus.RELEASED) {
            throw new InvalidReservationStateException("해제된 예약은 확정할 수 없습니다");
        }
        
        this.status = ReservationStatus.CONFIRMED;
        markAsUpdated();
    }
    
    private void validateCreate(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt,
            LocalDateTime currentTime
    ) {
        if (id == null) {
            throw new InvalidReservationException("Reservation ID는 필수입니다");
        }
        
        if (skuId == null) {
            throw new InvalidReservationException("SKU ID는 필수입니다");
        }
        
        if (quantity == null || quantity.value() == 0) {
            throw new InvalidReservationException("수량은 0보다 커야 합니다");
        }
        
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new InvalidReservationException("주문 ID는 필수입니다");
        }
        
        if (expiresAt == null) {
            throw new InvalidReservationException("만료 시간은 필수입니다");
        }
        
        if (expiresAt.isBefore(currentTime)) {
            throw new InvalidReservationException("만료 시간은 현재 시간 이후여야 합니다");
        }
    }
    
    private void validateRestore(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt,
            ReservationStatus status,
            LocalDateTime createdAt,
            Long version
    ) {
        if (id == null) {
            throw new InvalidReservationException("Reservation ID는 필수입니다");
        }
        
        if (skuId == null) {
            throw new InvalidReservationException("SKU ID는 필수입니다");
        }
        
        if (quantity == null || quantity.value() == 0) {
            throw new InvalidReservationException("수량은 0보다 커야 합니다");
        }
        
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new InvalidReservationException("주문 ID는 필수입니다");
        }
        
        if (expiresAt == null) {
            throw new InvalidReservationException("만료 시간은 필수입니다");
        }
        
        if (status == null) {
            throw new InvalidReservationException("예약 상태는 필수입니다");
        }
        
        if (createdAt == null) {
            throw new InvalidReservationException("생성 시간은 필수입니다");
        }
        
        if (version == null) {
            throw new InvalidReservationException("버전 정보는 필수입니다");
        }
    }
    
    @Override
    public ReservationId getId() {
        return id;
    }
}