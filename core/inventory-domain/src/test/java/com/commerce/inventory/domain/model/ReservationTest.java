package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;

import com.commerce.inventory.domain.exception.InvalidReservationException;
import com.commerce.inventory.domain.exception.InvalidReservationStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {
    
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 1, 1, 12, 0);

    @Test
    @DisplayName("유효한 값으로 Reservation을 생성할 수 있다")
    void shouldCreateReservationWithValidValues() {
        // given
        ReservationId id = ReservationId.generate();
        SkuId skuId = SkuId.generate();
        Quantity quantity = Quantity.of(10);
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = FIXED_TIME.plusHours(1);

        // when
        Reservation reservation = Reservation.create(id, skuId, quantity, orderId, expiresAt, FIXED_TIME);

        // then
        assertThat(reservation.getId()).isEqualTo(id);
        assertThat(reservation.getSkuId()).isEqualTo(skuId);
        assertThat(reservation.getQuantity()).isEqualTo(quantity);
        assertThat(reservation.getOrderId()).isEqualTo(orderId);
        assertThat(reservation.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("TTL(초)을 사용하여 Reservation을 생성할 수 있다")
    void shouldCreateReservationWithTTL() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity quantity = Quantity.of(10);
        String orderId = "ORDER-2024-001";
        int ttlSeconds = 3600; // 1시간

        // when
        Reservation reservation = Reservation.createWithTTL(skuId, quantity, orderId, ttlSeconds, FIXED_TIME);

        // then
        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getExpiresAt()).isEqualTo(FIXED_TIME.plusSeconds(ttlSeconds));
        assertThat(reservation.isActive(FIXED_TIME)).isTrue();
    }

    @Test
    @DisplayName("ReservationId가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenIdIsNull() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity quantity = Quantity.of(10);
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = FIXED_TIME.plusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(null, skuId, quantity, orderId, expiresAt, FIXED_TIME))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("Reservation ID는 필수입니다");
    }

    @Test
    @DisplayName("SkuId가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenSkuIdIsNull() {
        // given
        ReservationId id = ReservationId.generate();
        Quantity quantity = Quantity.of(10);
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = FIXED_TIME.plusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(id, null, quantity, orderId, expiresAt, FIXED_TIME))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("SKU ID는 필수입니다");
    }

    @Test
    @DisplayName("수량이 null이거나 0이면 예외가 발생한다")
    void shouldThrowExceptionWhenQuantityIsNullOrZero() {
        // given
        ReservationId id = ReservationId.generate();
        SkuId skuId = SkuId.generate();
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = FIXED_TIME.plusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(id, skuId, null, orderId, expiresAt, FIXED_TIME))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("수량은 0보다 커야 합니다");

        assertThatThrownBy(() -> Reservation.create(id, skuId, Quantity.of(0), orderId, expiresAt, FIXED_TIME))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("수량은 0보다 커야 합니다");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("주문 ID가 없거나 빈 값이면 예외가 발생한다")
    void shouldThrowExceptionWhenOrderIdIsNullOrEmpty(String invalidOrderId) {
        // given
        ReservationId id = ReservationId.generate();
        SkuId skuId = SkuId.generate();
        Quantity quantity = Quantity.of(10);
        LocalDateTime expiresAt = FIXED_TIME.plusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(id, skuId, quantity, invalidOrderId, expiresAt, FIXED_TIME))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("주문 ID는 필수입니다");
    }

    @Test
    @DisplayName("만료 시간이 현재 시간보다 이전이면 예외가 발생한다")
    void shouldThrowExceptionWhenExpiresAtIsInPast() {
        // given
        ReservationId id = ReservationId.generate();
        SkuId skuId = SkuId.generate();
        Quantity quantity = Quantity.of(10);
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = FIXED_TIME.minusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(id, skuId, quantity, orderId, expiresAt, FIXED_TIME))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("만료 시간은 현재 시간 이후여야 합니다");
    }

    @Test
    @DisplayName("활성 상태의 예약인지 확인할 수 있다")
    void shouldCheckIfReservationIsActive() {
        // given
        Reservation activeReservation = Reservation.createWithTTL(
                SkuId.generate(), Quantity.of(10), "ORDER-2024-001", 3600, FIXED_TIME
        );
        
        Reservation expiredReservation = Reservation.create(
                ReservationId.generate(),
                SkuId.generate(),
                Quantity.of(10),
                "ORDER-2024-002",
                FIXED_TIME.minusHours(1),
                FIXED_TIME.minusHours(2)  // 생성 시점을 만료 시간보다 이전으로 설정
        );

        // when & then
        assertThat(activeReservation.isActive(FIXED_TIME)).isTrue();
        assertThat(expiredReservation.isExpired(FIXED_TIME)).isTrue();
        assertThat(expiredReservation.isActive(FIXED_TIME)).isFalse();
    }

    @Test
    @DisplayName("예약을 해제할 수 있다")
    void shouldReleaseReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), Quantity.of(10), "ORDER-2024-001", 3600, FIXED_TIME
        );

        // when
        reservation.release();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(reservation.isActive(FIXED_TIME)).isFalse();
    }

    @Test
    @DisplayName("이미 해제된 예약을 다시 해제하면 예외가 발생한다")
    void shouldThrowExceptionWhenReleaseAlreadyReleasedReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), Quantity.of(10), "ORDER-2024-001", 3600, FIXED_TIME
        );
        reservation.release();

        // when & then
        assertThatThrownBy(() -> reservation.release())
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("이미 해제된 예약입니다");
    }

    @Test
    @DisplayName("예약을 확정할 수 있다")
    void shouldConfirmReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), Quantity.of(10), "ORDER-2024-001", 3600, FIXED_TIME
        );

        // when
        reservation.confirm(FIXED_TIME);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.isActive(FIXED_TIME)).isFalse();
    }

    @Test
    @DisplayName("만료된 예약을 확정하면 예외가 발생한다")
    void shouldThrowExceptionWhenConfirmExpiredReservation() {
        // given
        Reservation reservation = Reservation.create(
                ReservationId.generate(),
                SkuId.generate(),
                Quantity.of(10),
                "ORDER-2024-001",
                FIXED_TIME.minusHours(1),  // 이미 만료된 시간
                FIXED_TIME.minusHours(2)  // 생성 시점을 만료 시간보다 이전으로 설정
        );

        // when & then
        assertThatThrownBy(() -> reservation.confirm(FIXED_TIME))
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("만료된 예약은 확정할 수 없습니다");
    }

    @Test
    @DisplayName("이미 확정된 예약을 다시 확정하면 예외가 발생한다")
    void shouldThrowExceptionWhenConfirmAlreadyConfirmedReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), Quantity.of(10), "ORDER-2024-001", 3600, FIXED_TIME
        );
        reservation.confirm(FIXED_TIME);

        // when & then
        assertThatThrownBy(() -> reservation.confirm(FIXED_TIME))
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("이미 확정된 예약입니다");
    }

    @Test
    @DisplayName("해제된 예약을 확정하면 예외가 발생한다")
    void shouldThrowExceptionWhenConfirmReleasedReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), Quantity.of(10), "ORDER-2024-001", 3600, FIXED_TIME
        );
        reservation.release();

        // when & then
        assertThatThrownBy(() -> reservation.confirm(FIXED_TIME))
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("해제된 예약은 확정할 수 없습니다");
    }
}