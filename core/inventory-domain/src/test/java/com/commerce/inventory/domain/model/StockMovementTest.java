package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidStockMovementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockMovementTest {

    @Test
    @DisplayName("유효한 값으로 StockMovement를 생성할 수 있다")
    void shouldCreateStockMovementWithValidValues() {
        // given
        MovementId id = MovementId.generate();
        SkuId skuId = SkuId.generate();
        MovementType type = MovementType.INBOUND;
        Quantity quantity = new Quantity(100);
        String reference = "PO-2024-001";
        LocalDateTime timestamp = LocalDateTime.now();

        // when
        StockMovement movement = StockMovement.create(
                id, skuId, type, quantity, reference, timestamp
        );

        // then
        assertThat(movement.getId()).isEqualTo(id);
        assertThat(movement.getSkuId()).isEqualTo(skuId);
        assertThat(movement.getType()).isEqualTo(type);
        assertThat(movement.getQuantity()).isEqualTo(quantity);
        assertThat(movement.getReference()).isEqualTo(reference);
        assertThat(movement.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("입고 StockMovement를 생성할 수 있다")
    void shouldCreateInboundMovement() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity quantity = new Quantity(100);
        String reference = "PO-2024-001";

        // when
        StockMovement movement = StockMovement.createInbound(skuId, quantity, reference);

        // then
        assertThat(movement.getId()).isNotNull();
        assertThat(movement.getType()).isEqualTo(MovementType.INBOUND);
        assertThat(movement.isInbound()).isTrue();
        assertThat(movement.isOutbound()).isFalse();
    }

    @Test
    @DisplayName("출고 StockMovement를 생성할 수 있다")
    void shouldCreateOutboundMovement() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity quantity = new Quantity(50);
        String reference = "ORDER-2024-001";

        // when
        StockMovement movement = StockMovement.createOutbound(skuId, quantity, reference);

        // then
        assertThat(movement.getId()).isNotNull();
        assertThat(movement.getType()).isEqualTo(MovementType.OUTBOUND);
        assertThat(movement.isOutbound()).isTrue();
        assertThat(movement.isInbound()).isFalse();
    }

    @Test
    @DisplayName("MovementId가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenIdIsNull() {
        // given
        SkuId skuId = SkuId.generate();
        MovementType type = MovementType.INBOUND;
        Quantity quantity = new Quantity(100);
        String reference = "PO-2024-001";

        // when & then
        assertThatThrownBy(() -> StockMovement.create(
                null, skuId, type, quantity, reference, LocalDateTime.now()
        ))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("Movement ID는 필수입니다");
    }

    @Test
    @DisplayName("SkuId가 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenSkuIdIsNull() {
        // given
        MovementId id = MovementId.generate();
        MovementType type = MovementType.INBOUND;
        Quantity quantity = new Quantity(100);
        String reference = "PO-2024-001";

        // when & then
        assertThatThrownBy(() -> StockMovement.create(
                id, null, type, quantity, reference, LocalDateTime.now()
        ))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("SKU ID는 필수입니다");
    }

    @Test
    @DisplayName("MovementType이 null이면 예외가 발생한다")
    void shouldThrowExceptionWhenTypeIsNull() {
        // given
        MovementId id = MovementId.generate();
        SkuId skuId = SkuId.generate();
        Quantity quantity = new Quantity(100);
        String reference = "PO-2024-001";

        // when & then
        assertThatThrownBy(() -> StockMovement.create(
                id, skuId, null, quantity, reference, LocalDateTime.now()
        ))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("Movement 타입은 필수입니다");
    }

    @Test
    @DisplayName("수량이 null이거나 0이면 예외가 발생한다")
    void shouldThrowExceptionWhenQuantityIsNullOrZero() {
        // given
        MovementId id = MovementId.generate();
        SkuId skuId = SkuId.generate();
        MovementType type = MovementType.INBOUND;
        String reference = "PO-2024-001";

        // when & then
        assertThatThrownBy(() -> StockMovement.create(
                id, skuId, type, null, reference, LocalDateTime.now()
        ))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("수량은 0보다 커야 합니다");

        assertThatThrownBy(() -> StockMovement.create(
                id, skuId, type, new Quantity(0), reference, LocalDateTime.now()
        ))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("수량은 0보다 커야 합니다");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("참조 번호가 없거나 빈 값이면 예외가 발생한다")
    void shouldThrowExceptionWhenReferenceIsNullOrEmpty(String invalidReference) {
        // given
        MovementId id = MovementId.generate();
        SkuId skuId = SkuId.generate();
        MovementType type = MovementType.INBOUND;
        Quantity quantity = new Quantity(100);

        // when & then
        assertThatThrownBy(() -> StockMovement.create(
                id, skuId, type, quantity, invalidReference, LocalDateTime.now()
        ))
                .isInstanceOf(InvalidStockMovementException.class)
                .hasMessage("참조 번호는 필수입니다");
    }

    @ParameterizedTest
    @EnumSource(MovementType.class)
    @DisplayName("모든 MovementType으로 StockMovement를 생성할 수 있다")
    void shouldCreateStockMovementWithAllTypes(MovementType type) {
        // given
        SkuId skuId = SkuId.generate();
        Quantity quantity = new Quantity(100);
        String reference = "REF-2024-001";

        // when
        StockMovement movement = StockMovement.create(
                MovementId.generate(), skuId, type, quantity, reference, LocalDateTime.now()
        );

        // then
        assertThat(movement.getType()).isEqualTo(type);
    }

    @Test
    @DisplayName("조정 타입의 StockMovement를 생성할 수 있다")
    void shouldCreateAdjustmentMovement() {
        // given
        SkuId skuId = SkuId.generate();
        Quantity quantity = new Quantity(10);
        String reference = "ADJ-2024-001";

        // when
        StockMovement movement = StockMovement.createAdjustment(skuId, quantity, reference);

        // then
        assertThat(movement.getType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(movement.isInbound()).isFalse();
        assertThat(movement.isOutbound()).isFalse();
    }
}