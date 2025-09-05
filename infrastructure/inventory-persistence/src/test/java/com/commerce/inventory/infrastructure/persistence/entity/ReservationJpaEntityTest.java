package com.commerce.inventory.infrastructure.persistence.entity;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.ReservationStatus;
import com.commerce.inventory.domain.model.SkuId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationJpaEntityTest {

    @Test
    @DisplayName("도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void should_convert_from_domain_model() {
        // given
        ReservationId id = ReservationId.generate();
        SkuId skuId = SkuId.of("SKU-123");
        Quantity quantity = Quantity.of(5);
        String orderId = "ORDER-123";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        ReservationStatus status = ReservationStatus.ACTIVE;
        LocalDateTime createdAt = LocalDateTime.now();
        Long version = 0L;
        
        Reservation reservation = Reservation.restore(
                id,
                skuId,
                quantity,
                orderId,
                expiresAt,
                status,
                createdAt,
                version
        );

        // when
        ReservationJpaEntity entity = ReservationJpaEntity.fromDomainModel(reservation);

        // then
        assertThat(entity.getId()).isEqualTo(id.value());
        assertThat(entity.getSkuId()).isEqualTo(skuId.value());
        assertThat(entity.getInventoryId()).isEqualTo(skuId.value()); // SKU ID를 inventory ID로 사용
        assertThat(entity.getQuantity()).isEqualTo(quantity.value());
        assertThat(entity.getOrderId()).isEqualTo(orderId);
        assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entity.getStatus()).isEqualTo(ReservationJpaEntity.ReservationStatus.ACTIVE);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(reservation.getUpdatedAt());
        assertThat(entity.getVersion()).isEqualTo(version);
    }

    @Test
    @DisplayName("JPA 엔티티를 도메인 모델로 변환할 수 있다")
    void should_convert_to_domain_model() {
        // given
        String id = "RES-123";
        String skuId = "SKU-123";
        Integer quantity = 5;
        String orderId = "ORDER-123";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        ReservationJpaEntity.ReservationStatus status = ReservationJpaEntity.ReservationStatus.ACTIVE;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        Long version = 1L;
        
        ReservationJpaEntity entity = ReservationJpaEntity.builder()
                .id(id)
                .skuId(skuId)
                .inventoryId(skuId)
                .quantity(quantity)
                .orderId(orderId)
                .expiresAt(expiresAt)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version)
                .build();

        // when
        Reservation reservation = entity.toDomainModel();

        // then
        assertThat(reservation.getId()).isEqualTo(new ReservationId(id));
        assertThat(reservation.getSkuId()).isEqualTo(new SkuId(skuId));
        assertThat(reservation.getQuantity()).isEqualTo(Quantity.of(quantity));
        assertThat(reservation.getOrderId()).isEqualTo(orderId);
        assertThat(reservation.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reservation.getVersion()).isEqualTo(version);
    }

    @Test
    @DisplayName("다양한 상태의 예약을 정확히 변환할 수 있다")
    void should_convert_different_reservation_statuses() {
        // given
        testStatusConversion(ReservationStatus.ACTIVE, ReservationJpaEntity.ReservationStatus.ACTIVE);
        testStatusConversion(ReservationStatus.CONFIRMED, ReservationJpaEntity.ReservationStatus.CONFIRMED);
        testStatusConversion(ReservationStatus.RELEASED, ReservationJpaEntity.ReservationStatus.RELEASED);
        testStatusConversion(ReservationStatus.EXPIRED, ReservationJpaEntity.ReservationStatus.EXPIRED);
    }
    
    private void testStatusConversion(ReservationStatus domainStatus, ReservationJpaEntity.ReservationStatus expectedJpaStatus) {
        // given
        Reservation reservation = Reservation.restore(
                ReservationId.generate(),
                SkuId.of("SKU-123"),
                Quantity.of(5),
                "ORDER-123",
                LocalDateTime.now().plusMinutes(30),
                domainStatus,
                LocalDateTime.now(),
                0L
        );

        // when
        ReservationJpaEntity entity = ReservationJpaEntity.fromDomainModel(reservation);
        
        // then
        assertThat(entity.getStatus()).isEqualTo(expectedJpaStatus);
        
        // when - reverse conversion
        Reservation restoredReservation = entity.toDomainModel();
        
        // then
        assertThat(restoredReservation.getStatus()).isEqualTo(domainStatus);
    }

    @Test
    @DisplayName("신규 예약 생성시 도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void should_convert_new_reservation_from_domain_model() {
        // given
        SkuId skuId = SkuId.of("SKU-456");
        Quantity quantity = Quantity.of(10);
        String orderId = "ORDER-456";
        int ttlSeconds = 1800; // 30분
        
        Reservation reservation = Reservation.create(skuId, quantity, orderId, ttlSeconds);

        // when
        ReservationJpaEntity entity = ReservationJpaEntity.fromDomainModel(reservation);

        // then
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getSkuId()).isEqualTo(skuId.value());
        assertThat(entity.getInventoryId()).isEqualTo(skuId.value());
        assertThat(entity.getQuantity()).isEqualTo(quantity.value());
        assertThat(entity.getOrderId()).isEqualTo(orderId);
        assertThat(entity.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(entity.getStatus()).isEqualTo(ReservationJpaEntity.ReservationStatus.ACTIVE);
        assertThat(entity.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("만료된 예약을 정확히 복원할 수 있다")
    void should_correctly_restore_expired_reservation() {
        // given
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(1);
        ReservationJpaEntity entity = ReservationJpaEntity.builder()
                .id("RES-999")
                .skuId("SKU-999")
                .inventoryId("SKU-999")
                .quantity(3)
                .orderId("ORDER-999")
                .expiresAt(expiredTime)
                .status(ReservationJpaEntity.ReservationStatus.EXPIRED)
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .version(2L)
                .build();

        // when
        Reservation reservation = entity.toDomainModel();

        // then
        assertThat(reservation.isExpired(LocalDateTime.now())).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }
}