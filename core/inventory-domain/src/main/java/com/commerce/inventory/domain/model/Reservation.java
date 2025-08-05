package com.commerce.inventory.domain.model;

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
    
    private Reservation(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt
    ) {
        validateCreate(id, skuId, quantity, orderId, expiresAt);
        
        this.id = id;
        this.skuId = skuId;
        this.quantity = quantity;
        this.orderId = orderId;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }
    
    public static Reservation create(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt
    ) {
        return new Reservation(id, skuId, quantity, orderId, expiresAt);
    }
    
    public static Reservation createWithTTL(
            SkuId skuId,
            Quantity quantity,
            String orderId,
            int ttlSeconds
    ) {
        return new Reservation(
                ReservationId.generate(),
                skuId,
                quantity,
                orderId,
                LocalDateTime.now().plusSeconds(ttlSeconds)
        );
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isActive() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }
    
    public void release() {
        if (status == ReservationStatus.RELEASED) {
            throw new InvalidReservationStateException("이미 해제된 예약입니다");
        }
        
        this.status = ReservationStatus.RELEASED;
    }
    
    public void confirm() {
        if (isExpired()) {
            throw new InvalidReservationStateException("만료된 예약은 확정할 수 없습니다");
        }
        
        if (status == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException("이미 확정된 예약입니다");
        }
        
        if (status == ReservationStatus.RELEASED) {
            throw new InvalidReservationStateException("해제된 예약은 확정할 수 없습니다");
        }
        
        this.status = ReservationStatus.CONFIRMED;
    }
    
    private void validateCreate(
            ReservationId id,
            SkuId skuId,
            Quantity quantity,
            String orderId,
            LocalDateTime expiresAt
    ) {
        if (id == null) {
            throw new InvalidReservationException("Reservation ID는 필수입니다");
        }
        
        if (skuId == null) {
            throw new InvalidReservationException("SKU ID는 필수입니다");
        }
        
        if (quantity == null || quantity.getValue() == 0) {
            throw new InvalidReservationException("수량은 0보다 커야 합니다");
        }
        
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new InvalidReservationException("주문 ID는 필수입니다");
        }
        
        if (expiresAt == null) {
            throw new InvalidReservationException("만료 시간은 필수입니다");
        }
        
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new InvalidReservationException("만료 시간은 현재 시간 이후여야 합니다");
        }
    }
}