package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.MovementId;
import com.commerce.inventory.domain.model.MovementType;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.model.StockMovement;
import com.commerce.inventory.infrastructure.persistence.entity.StockMovementJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementPersistenceAdapterTest {
    
    @Mock
    private StockMovementJpaRepository stockMovementJpaRepository;
    
    @InjectMocks
    private StockMovementPersistenceAdapter adapter;
    
    private StockMovement stockMovement;
    private StockMovementJpaEntity jpaEntity;
    
    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        stockMovement = StockMovement.createInbound(
                SkuId.of("SKU123"),
                Quantity.of(100),
                "Initial stock",
                now
        );
        
        jpaEntity = StockMovementJpaEntity.fromDomainModel(stockMovement);
    }
    
    @Test
    @DisplayName("재고 이동 기록을 저장할 수 있다")
    void save() {
        // Given
        when(stockMovementJpaRepository.save(any(StockMovementJpaEntity.class)))
                .thenReturn(jpaEntity);
        
        // When
        adapter.save(stockMovement);
        
        // Then
        ArgumentCaptor<StockMovementJpaEntity> captor = ArgumentCaptor.forClass(StockMovementJpaEntity.class);
        verify(stockMovementJpaRepository).save(captor.capture());
        
        StockMovementJpaEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getMovementId()).isEqualTo(stockMovement.getId().value());
        assertThat(savedEntity.getSkuId()).isEqualTo(stockMovement.getSkuId().value());
        assertThat(savedEntity.getQuantity()).isEqualTo(stockMovement.getQuantity().value());
        assertThat(savedEntity.getMovementType()).isEqualTo(stockMovement.getType());
        assertThat(savedEntity.getReference()).isEqualTo(stockMovement.getReference());
        assertThat(savedEntity.getTimestamp()).isEqualTo(stockMovement.getTimestamp());
    }
    
    @Test
    @DisplayName("출고 이동 기록을 저장할 수 있다")
    void saveShipmentMovement() {
        // Given
        LocalDateTime shipmentTime = LocalDateTime.now();
        StockMovement shipmentMovement = StockMovement.createOutbound(
                SkuId.of("SKU456"),
                Quantity.of(50),
                "Order fulfilled",
                shipmentTime
        );
        
        StockMovementJpaEntity shipmentEntity = StockMovementJpaEntity.fromDomainModel(shipmentMovement);
        when(stockMovementJpaRepository.save(any(StockMovementJpaEntity.class)))
                .thenReturn(shipmentEntity);
        
        // When
        adapter.save(shipmentMovement);
        
        // Then
        ArgumentCaptor<StockMovementJpaEntity> captor = ArgumentCaptor.forClass(StockMovementJpaEntity.class);
        verify(stockMovementJpaRepository).save(captor.capture());
        
        StockMovementJpaEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getMovementId()).isEqualTo(shipmentMovement.getId().value());
        assertThat(savedEntity.getSkuId()).isEqualTo(shipmentMovement.getSkuId().value());
        assertThat(savedEntity.getQuantity()).isEqualTo(shipmentMovement.getQuantity().value());
        assertThat(savedEntity.getMovementType()).isEqualTo(shipmentMovement.getType());
        assertThat(savedEntity.getReference()).isEqualTo(shipmentMovement.getReference());
        assertThat(savedEntity.getTimestamp()).isEqualTo(shipmentMovement.getTimestamp());
    }
}