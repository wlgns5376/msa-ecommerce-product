package com.commerce.inventory.domain.model;

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

    @Test
    @DisplayName("유효한 값으로 Reservation을 생성할 수 있다")
    void shouldCreateReservationWithValidValues() {
        // given
        ReservationId id = ReservationId.generate();
        SkuId skuId = SkuId.generate();
        Quantity quantity = new Quantity(10);
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // when
        Reservation reservation = Reservation.create(id, skuId, quantity, orderId, expiresAt);

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
        Quantity quantity = new Quantity(10);
        String orderId = "ORDER-2024-001";
        int ttlSeconds = 3600; // 1시간

        // when
        Reservation reservation = Reservation.createWithTTL(skuId, quantity, orderId, ttlSeconds);

        // then
        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(reservation.getExpiresAt()).isBefore(LocalDateTime.now().plusSeconds(ttlSeconds + 1));
        assertThat(reservation.isActive()).isTrue();
    }

    @Test
    @DisplayName("ReservationId가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenIdIsNull() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity quantity = new Quantity(10);
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(null, skuId, quantity, orderId, expiresAt))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("Reservation ID는 필수입니다");
    }

    @Test
    @DisplayName("SkuId가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenSkuIdIsNull() {
        // given
        ReservationId id = ReservationId.generate();
        Quantity quantity = new Quantity(10);
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(id, null, quantity, orderId, expiresAt))
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
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(id, skuId, null, orderId, expiresAt))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("수량은 0보다 커야 합니다");

        assertThatThrownBy(() -> Reservation.create(id, skuId, new Quantity(0), orderId, expiresAt))
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
        Quantity quantity = new Quantity(10);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(id, skuId, quantity, invalidOrderId, expiresAt))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("주문 ID는 필수입니다");
    }

    @Test
    @DisplayName("만료 시간이 현재 시간보다 이전이면 예외가 발생한다")
    void shouldThrowExceptionWhenExpiresAtIsInPast() {
        // given
        ReservationId id = ReservationId.generate();
        SkuId skuId = SkuId.generate();
        Quantity quantity = new Quantity(10);
        String orderId = "ORDER-2024-001";
        LocalDateTime expiresAt = LocalDateTime.now().minusHours(1);

        // when & then
        assertThatThrownBy(() -> Reservation.create(id, skuId, quantity, orderId, expiresAt))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessage("만료 시간은 현재 시간 이후여야 합니다");
    }

    @Test
    @DisplayName("활성 상태의 예약인지 확인할 수 있다")
    void shouldCheckIfReservationIsActive() {
        // given
        Reservation activeReservation = Reservation.createWithTTL(
                SkuId.generate(), new Quantity(10), "ORDER-2024-001", 3600
        );
        
        Reservation expiredReservation = Reservation.create(
                ReservationId.generate(),
                SkuId.generate(),
                new Quantity(10),
                "ORDER-2024-002",
                LocalDateTime.now().plusSeconds(1)
        );

        // when & then
        assertThat(activeReservation.isActive()).isTrue();
        
        // 만료 시간까지 대기
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        assertThat(expiredReservation.isExpired()).isTrue();
        assertThat(expiredReservation.isActive()).isFalse();
    }

    @Test
    @DisplayName("예약을 해제할 수 있다")
    void shouldReleaseReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), new Quantity(10), "ORDER-2024-001", 3600
        );

        // when
        reservation.release();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(reservation.isActive()).isFalse();
    }

    @Test
    @DisplayName("이미 해제된 예약을 다시 해제하면 예외가 발생한다")
    void shouldThrowExceptionWhenReleaseAlreadyReleasedReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), new Quantity(10), "ORDER-2024-001", 3600
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
                SkuId.generate(), new Quantity(10), "ORDER-2024-001", 3600
        );

        // when
        reservation.confirm();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.isActive()).isFalse();
    }

    @Test
    @DisplayName("만료된 예약을 확정하면 예외가 발생한다")
    void shouldThrowExceptionWhenConfirmExpiredReservation() {
        // given
        Reservation reservation = Reservation.create(
                ReservationId.generate(),
                SkuId.generate(),
                new Quantity(10),
                "ORDER-2024-001",
                LocalDateTime.now().plusNanos(100_000_000)
        );
        
        // 만료 시간까지 대기
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // ignore
        }

        // when & then
        assertThatThrownBy(() -> reservation.confirm())
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("만료된 예약은 확정할 수 없습니다");
    }

    @Test
    @DisplayName("이미 확정된 예약을 다시 확정하면 예외가 발생한다")
    void shouldThrowExceptionWhenConfirmAlreadyConfirmedReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), new Quantity(10), "ORDER-2024-001", 3600
        );
        reservation.confirm();

        // when & then
        assertThatThrownBy(() -> reservation.confirm())
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("이미 확정된 예약입니다");
    }

    @Test
    @DisplayName("해제된 예약을 확정하면 예외가 발생한다")
    void shouldThrowExceptionWhenConfirmReleasedReservation() {
        // given
        Reservation reservation = Reservation.createWithTTL(
                SkuId.generate(), new Quantity(10), "ORDER-2024-001", 3600
        );
        reservation.release();

        // when & then
        assertThatThrownBy(() -> reservation.confirm())
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("해제된 예약은 확정할 수 없습니다");
    }
}