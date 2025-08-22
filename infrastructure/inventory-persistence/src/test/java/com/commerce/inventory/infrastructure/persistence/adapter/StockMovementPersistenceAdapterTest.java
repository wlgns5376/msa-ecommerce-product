package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.model.StockMovement;
import com.commerce.inventory.infrastructure.persistence.entity.StockMovementJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockMovementPersistenceAdapterTest {
    
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2024, 1, 1, 10, 0);
    
    @Mock
    private StockMovementJpaRepository stockMovementJpaRepository;
    
    @InjectMocks
    private StockMovementPersistenceAdapter adapter;
    
    private void assertMovementIsSavedCorrectly(StockMovement movement) {
        // When
        adapter.save(movement);

        // Then
        ArgumentCaptor<StockMovementJpaEntity> captor = ArgumentCaptor.forClass(StockMovementJpaEntity.class);
        verify(stockMovementJpaRepository).save(captor.capture());

        StockMovementJpaEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getMovementId()).isEqualTo(movement.getId().value());
        assertThat(savedEntity.getSkuId()).isEqualTo(movement.getSkuId().value());
        assertThat(savedEntity.getQuantity()).isEqualTo(movement.getQuantity().value());
        assertThat(savedEntity.getMovementType()).isEqualTo(movement.getType());
        assertThat(savedEntity.getReference()).isEqualTo(movement.getReference());
        assertThat(savedEntity.getTimestamp()).isEqualTo(movement.getTimestamp());
    }

    @DisplayName("이동 기록을 저장할 수 있다")
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("movementProvider")
    void saveMovement(StockMovement movement, String testName) {
        // When & Then
        assertMovementIsSavedCorrectly(movement);
    }

    private static Stream<Arguments> movementProvider() {
        return Stream.of(
                Arguments.of(StockMovement.create(
                        SkuId.of("SKU123"),
                        Quantity.of(100),
                        com.commerce.inventory.domain.model.MovementType.RECEIVE,
                        "PO-123",
                        FIXED_TIME
                ), "재고 입고(RECEIVE) 이동 기록"),
                Arguments.of(StockMovement.createOutbound(
                        SkuId.of("SKU456"),
                        Quantity.of(50),
                        "Order fulfilled",
                        FIXED_TIME
                ), "출고(OUTBOUND) 이동 기록")
        );
    }
}
