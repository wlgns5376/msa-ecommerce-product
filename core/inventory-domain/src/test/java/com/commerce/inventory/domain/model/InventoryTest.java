package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryTest {

    @Test
    @DisplayName("유효한 값으로 Inventory를 생성할 수 있다")
    void shouldCreateInventoryWithValidValues() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity totalQuantity = new Quantity(100);
        Quantity reservedQuantity = new Quantity(20);

        // when
        Inventory inventory = Inventory.create(skuId, totalQuantity, reservedQuantity);

        // then
        assertThat(inventory.getSkuId()).isEqualTo(skuId);
        assertThat(inventory.getTotalQuantity()).isEqualTo(totalQuantity);
        assertThat(inventory.getReservedQuantity()).isEqualTo(reservedQuantity);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(new Quantity(80));
    }

    @Test
    @DisplayName("초기 재고로 Inventory를 생성할 수 있다")
    void shouldCreateInventoryWithInitialStock() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity initialQuantity = new Quantity(100);

        // when
        Inventory inventory = Inventory.createWithInitialStock(skuId, initialQuantity);

        // then
        assertThat(inventory.getSkuId()).isEqualTo(skuId);
        assertThat(inventory.getTotalQuantity()).isEqualTo(initialQuantity);
        assertThat(inventory.getReservedQuantity()).isEqualTo(new Quantity(0));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(initialQuantity);
    }

    @Test
    @DisplayName("재고 없이 Inventory를 생성할 수 있다")
    void shouldCreateEmptyInventory() {
        // given
        SkuId skuId = SkuId.generate();

        // when
        Inventory inventory = Inventory.createEmpty(skuId);

        // then
        assertThat(inventory.getSkuId()).isEqualTo(skuId);
        assertThat(inventory.getTotalQuantity()).isEqualTo(new Quantity(0));
        assertThat(inventory.getReservedQuantity()).isEqualTo(new Quantity(0));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(new Quantity(0));
    }

    @Test
    @DisplayName("SKU ID가 없으면 Inventory 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenSkuIdIsNull() {
        // given
        Quantity totalQuantity = new Quantity(100);
        Quantity reservedQuantity = new Quantity(20);

        // when & then
        assertThatThrownBy(() -> Inventory.create(null, totalQuantity, reservedQuantity))
                .isInstanceOf(InvalidInventoryException.class)
                .hasMessage("SKU ID는 필수입니다");
    }

    @Test
    @DisplayName("예약 수량이 총 수량보다 크면 Inventory 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenReservedQuantityExceedsTotal() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity totalQuantity = new Quantity(50);
        Quantity reservedQuantity = new Quantity(100);

        // when & then
        assertThatThrownBy(() -> Inventory.create(skuId, totalQuantity, reservedQuantity))
                .isInstanceOf(InvalidInventoryException.class)
                .hasMessage("예약 수량은 총 수량을 초과할 수 없습니다");
    }

    @Test
    @DisplayName("재고를 입고할 수 있다")
    void shouldReceiveStock() {
        // given
        Inventory inventory = Inventory.createEmpty(SkuId.generate());
        Quantity receiveQuantity = new Quantity(50);
        String reference = "PO-2024-001";

        // when
        inventory.receive(receiveQuantity, reference);

        // then
        assertThat(inventory.getTotalQuantity()).isEqualTo(receiveQuantity);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(receiveQuantity);
    }

    @Test
    @DisplayName("재고를 여러 번 입고할 수 있다")
    void shouldReceiveStockMultipleTimes() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), new Quantity(100));
        Quantity firstReceive = new Quantity(50);
        Quantity secondReceive = new Quantity(30);

        // when
        inventory.receive(firstReceive, "PO-2024-001");
        inventory.receive(secondReceive, "PO-2024-002");

        // then
        assertThat(inventory.getTotalQuantity()).isEqualTo(new Quantity(180));
    }

    @Test
    @DisplayName("가용 재고를 확인할 수 있다")
    void shouldCheckAvailableStock() {
        // given
        Inventory inventory = Inventory.create(
                SkuId.generate(),
                new Quantity(100),
                new Quantity(30)
        );

        // when & then
        assertThat(inventory.canReserve(new Quantity(70))).isTrue();
        assertThat(inventory.canReserve(new Quantity(71))).isFalse();
    }

    @Test
    @DisplayName("재고를 예약할 수 있다")
    void shouldReserveStock() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), new Quantity(100));
        Quantity reserveQuantity = new Quantity(30);
        String orderId = "ORDER-2024-001";
        int ttlSeconds = 3600;

        // when
        ReservationId reservationId = inventory.reserve(reserveQuantity, orderId, ttlSeconds);

        // then
        assertThat(reservationId).isNotNull();
        assertThat(inventory.getReservedQuantity()).isEqualTo(reserveQuantity);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(new Quantity(70));
    }

    @Test
    @DisplayName("가용 재고가 부족하면 예약 시 예외가 발생한다")
    void shouldThrowExceptionWhenInsufficientStock() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), new Quantity(50));
        Quantity reserveQuantity = new Quantity(100);

        // when & then
        assertThatThrownBy(() -> inventory.reserve(reserveQuantity, "ORDER-2024-001", 3600))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("재고가 부족합니다. 가용 재고: 50, 요청 수량: 100");
    }

    @Test
    @DisplayName("예약을 해제할 수 있다")
    void shouldReleaseReservation() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), new Quantity(100));
        ReservationId reservationId = inventory.reserve(new Quantity(30), "ORDER-2024-001", 3600);

        // when
        inventory.releaseReservation(reservationId, new Quantity(30));

        // then
        assertThat(inventory.getReservedQuantity()).isEqualTo(new Quantity(0));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(new Quantity(100));
    }

    @Test
    @DisplayName("예약을 확정하여 재고를 차감할 수 있다")
    void shouldConfirmReservation() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), new Quantity(100));
        ReservationId reservationId = inventory.reserve(new Quantity(30), "ORDER-2024-001", 3600);

        // when
        inventory.confirmReservation(reservationId, new Quantity(30));

        // then
        assertThat(inventory.getTotalQuantity()).isEqualTo(new Quantity(70));
        assertThat(inventory.getReservedQuantity()).isEqualTo(new Quantity(0));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(new Quantity(70));
    }

    @Test
    @DisplayName("재고를 직접 차감할 수 있다")
    void shouldDeductStock() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), new Quantity(100));
        Quantity deductQuantity = new Quantity(30);
        String reference = "RETURN-2024-001";

        // when
        inventory.deduct(deductQuantity, reference);

        // then
        assertThat(inventory.getTotalQuantity()).isEqualTo(new Quantity(70));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(new Quantity(70));
    }

    @Test
    @DisplayName("총 재고보다 많은 수량을 차감하면 예외가 발생한다")
    void shouldThrowExceptionWhenDeductExceedsTotalStock() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), new Quantity(50));
        Quantity deductQuantity = new Quantity(100);

        // when & then
        assertThatThrownBy(() -> inventory.deduct(deductQuantity, "RETURN-2024-001"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("재고가 부족합니다. 총 재고: 50, 차감 요청: 100");
    }
}