package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;

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
        Quantity totalQuantity = Quantity.of(100);
        Quantity reservedQuantity = Quantity.of(20);

        // when
        Inventory inventory = Inventory.create(skuId, totalQuantity, reservedQuantity);

        // then
        assertThat(inventory.getSkuId()).isEqualTo(skuId);
        assertThat(inventory.getTotalQuantity()).isEqualTo(totalQuantity);
        assertThat(inventory.getReservedQuantity()).isEqualTo(reservedQuantity);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(Quantity.of(80));
    }

    @Test
    @DisplayName("초기 재고로 Inventory를 생성할 수 있다")
    void shouldCreateInventoryWithInitialStock() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity initialQuantity = Quantity.of(100);

        // when
        Inventory inventory = Inventory.createWithInitialStock(skuId, initialQuantity);

        // then
        assertThat(inventory.getSkuId()).isEqualTo(skuId);
        assertThat(inventory.getTotalQuantity()).isEqualTo(initialQuantity);
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(0));
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
        assertThat(inventory.getTotalQuantity()).isEqualTo(Quantity.of(0));
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(0));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(Quantity.of(0));
    }

    @Test
    @DisplayName("SKU ID가 없으면 Inventory 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenSkuIdIsNull() {
        // given
        Quantity totalQuantity = Quantity.of(100);
        Quantity reservedQuantity = Quantity.of(20);

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
        Quantity totalQuantity = Quantity.of(50);
        Quantity reservedQuantity = Quantity.of(100);

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
        Quantity receiveQuantity = Quantity.of(50);

        // when
        inventory.receive(receiveQuantity);

        // then
        assertThat(inventory.getTotalQuantity()).isEqualTo(receiveQuantity);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(receiveQuantity);
    }

    @Test
    @DisplayName("재고를 여러 번 입고할 수 있다")
    void shouldReceiveStockMultipleTimes() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        Quantity firstReceive = Quantity.of(50);
        Quantity secondReceive = Quantity.of(30);

        // when
        inventory.receive(firstReceive);
        inventory.receive(secondReceive);

        // then
        assertThat(inventory.getTotalQuantity()).isEqualTo(Quantity.of(180));
    }

    @Test
    @DisplayName("가용 재고를 확인할 수 있다")
    void shouldCheckAvailableStock() {
        // given
        Inventory inventory = Inventory.create(
                SkuId.generate(),
                Quantity.of(100),
                Quantity.of(30)
        );

        // when & then
        assertThat(inventory.canReserve(Quantity.of(70))).isTrue();
        assertThat(inventory.canReserve(Quantity.of(71))).isFalse();
    }

    @Test
    @DisplayName("재고를 예약할 수 있다")
    void shouldReserveStock() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        Quantity reserveQuantity = Quantity.of(30);
        String orderId = "ORDER-2024-001";
        int ttlSeconds = 3600;

        // when
        ReservationId reservationId = inventory.reserve(reserveQuantity, orderId, ttlSeconds);

        // then
        assertThat(reservationId).isNotNull();
        assertThat(inventory.getReservedQuantity()).isEqualTo(reserveQuantity);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(Quantity.of(70));
    }

    @Test
    @DisplayName("가용 재고가 부족하면 예약 시 예외가 발생한다")
    void shouldThrowExceptionWhenInsufficientStock() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(50));
        Quantity reserveQuantity = Quantity.of(100);

        // when & then
        assertThatThrownBy(() -> inventory.reserve(reserveQuantity, "ORDER-2024-001", 3600))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("재고가 부족합니다. 가용 재고: 50, 요청 수량: 100");
    }

    @Test
    @DisplayName("예약을 해제할 수 있다")
    void shouldReleaseReservation() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        ReservationId reservationId = inventory.reserve(Quantity.of(30), "ORDER-2024-001", 3600);

        // when
        inventory.releaseReservedQuantity(Quantity.of(30));

        // then
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(0));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(Quantity.of(100));
    }

    @Test
    @DisplayName("예약을 확정하여 재고를 차감할 수 있다")
    void shouldConfirmReservation() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        ReservationId reservationId = inventory.reserve(Quantity.of(30), "ORDER-2024-001", 3600);

        // when
        inventory.confirmReservedQuantity(Quantity.of(30));

        // then
        assertThat(inventory.getTotalQuantity()).isEqualTo(Quantity.of(70));
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(0));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(Quantity.of(70));
    }

    @Test
    @DisplayName("재고를 직접 차감할 수 있다")
    void shouldDeductStock() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        Quantity deductQuantity = Quantity.of(30);
        String reference = "RETURN-2024-001";

        // when
        inventory.deduct(deductQuantity, reference);

        // then
        assertThat(inventory.getTotalQuantity()).isEqualTo(Quantity.of(70));
        assertThat(inventory.getAvailableQuantity()).isEqualTo(Quantity.of(70));
    }

    @Test
    @DisplayName("총 재고보다 많은 수량을 차감하면 예외가 발생한다")
    void shouldThrowExceptionWhenDeductExceedsTotalStock() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(50));
        Quantity deductQuantity = Quantity.of(100);

        // when & then
        assertThatThrownBy(() -> inventory.deduct(deductQuantity, "RETURN-2024-001"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("재고가 부족합니다. 총 재고: 50, 차감 요청: 100");
    }
    
    @Test
    @DisplayName("예약 해제 시 해제할 수량이 0이면 예외가 발생한다")
    void shouldThrowExceptionWhenReleaseQuantityIsZero() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        inventory.reserve(Quantity.of(30), "ORDER-2024-001", 3600);
        
        // when & then
        assertThatThrownBy(() -> inventory.releaseReservedQuantity(Quantity.of(0)))
                .isInstanceOf(InvalidInventoryException.class)
                .hasMessage("해제할 수량은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("예약 해제 시 예약된 수량보다 많은 수량을 해제하면 예외가 발생한다")
    void shouldThrowExceptionWhenReleaseQuantityExceedsReserved() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        inventory.reserve(Quantity.of(30), "ORDER-2024-001", 3600);
        
        // when & then
        assertThatThrownBy(() -> inventory.releaseReservedQuantity(Quantity.of(50)))
                .isInstanceOf(InvalidInventoryException.class)
                .hasMessage("해제할 예약 수량이 부족합니다. 현재 예약: 30, 해제 요청: 50");
    }
    
    @Test
    @DisplayName("예약 확정 시 확정할 수량이 0이면 예외가 발생한다")
    void shouldThrowExceptionWhenConfirmQuantityIsZero() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        inventory.reserve(Quantity.of(30), "ORDER-2024-001", 3600);
        
        // when & then
        assertThatThrownBy(() -> inventory.confirmReservedQuantity(Quantity.of(0)))
                .isInstanceOf(InvalidInventoryException.class)
                .hasMessage("확정할 수량은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("예약 확정 시 예약된 수량보다 많은 수량을 확정하면 예외가 발생한다")
    void shouldThrowExceptionWhenConfirmQuantityExceedsReserved() {
        // given
        Inventory inventory = Inventory.createWithInitialStock(SkuId.generate(), Quantity.of(100));
        inventory.reserve(Quantity.of(30), "ORDER-2024-001", 3600);
        
        // when & then
        assertThatThrownBy(() -> inventory.confirmReservedQuantity(Quantity.of(50)))
                .isInstanceOf(InvalidInventoryException.class)
                .hasMessage("확정할 예약 수량이 부족합니다. 현재 예약: 30, 확정 요청: 50");
    }
    
    @Test
    @DisplayName("새로 생성된 Inventory의 version은 0이다")
    void shouldHaveInitialVersionZero() {
        // given
        SkuId skuId = SkuId.generate();
        
        // when
        Inventory inventory = Inventory.createEmpty(skuId);
        
        // then
        assertThat(inventory.getVersion()).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("restore 메소드로 특정 version의 Inventory를 복원할 수 있다")
    void shouldRestoreInventoryWithSpecificVersion() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity totalQuantity = Quantity.of(100);
        Quantity reservedQuantity = Quantity.of(20);
        Long version = 5L;
        
        // when
        Inventory inventory = Inventory.restore(skuId, totalQuantity, reservedQuantity, version);
        
        // then
        assertThat(inventory.getSkuId()).isEqualTo(skuId);
        assertThat(inventory.getTotalQuantity()).isEqualTo(totalQuantity);
        assertThat(inventory.getReservedQuantity()).isEqualTo(reservedQuantity);
        assertThat(inventory.getVersion()).isEqualTo(version);
    }
}