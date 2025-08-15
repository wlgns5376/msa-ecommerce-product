package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.infrastructure.persistence.entity.InventoryJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryPersistenceAdapter 테스트")
class InventoryPersistenceAdapterTest {
    
    @Mock
    private InventoryJpaRepository inventoryJpaRepository;
    
    private InventoryPersistenceAdapter adapter;
    
    @BeforeEach
    void setUp() {
        adapter = new InventoryPersistenceAdapter(inventoryJpaRepository);
    }
    
    @Test
    @DisplayName("SKU ID로 재고를 조회할 수 있다")
    void shouldLoadInventoryBySkuId() {
        // given
        SkuId skuId = SkuId.generate();
        InventoryJpaEntity entity = InventoryJpaEntity.builder()
                .skuId(skuId.value())
                .totalQuantity(100)
                .reservedQuantity(20)
                .version(3L)
                .build();
        
        when(inventoryJpaRepository.findById(skuId.value())).thenReturn(Optional.of(entity));
        
        // when
        Optional<Inventory> result = adapter.load(skuId);
        
        // then
        assertThat(result).isPresent();
        Inventory inventory = result.get();
        assertThat(inventory.getSkuId()).isEqualTo(skuId);
        assertThat(inventory.getTotalQuantity()).isEqualTo(Quantity.of(100));
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(20));
        assertThat(inventory.getVersion()).isEqualTo(3L);
    }
    
    @Test
    @DisplayName("존재하지 않는 SKU ID로 조회 시 빈 값을 반환한다")
    void shouldReturnEmptyWhenInventoryNotFound() {
        // given
        SkuId skuId = SkuId.generate();
        when(inventoryJpaRepository.findById(skuId.value())).thenReturn(Optional.empty());
        
        // when
        Optional<Inventory> result = adapter.load(skuId);
        
        // then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("재고를 저장할 수 있다")
    void shouldSaveInventory() {
        // given
        SkuId skuId = SkuId.generate();
        Inventory inventory = Inventory.restore(
                skuId, 
                Quantity.of(150), 
                Quantity.of(30),
                2L
        );
        
        // when
        adapter.save(inventory);
        
        // then
        ArgumentCaptor<InventoryJpaEntity> captor = ArgumentCaptor.forClass(InventoryJpaEntity.class);
        verify(inventoryJpaRepository).save(captor.capture());
        
        InventoryJpaEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getSkuId()).isEqualTo(skuId.value());
        assertThat(savedEntity.getTotalQuantity()).isEqualTo(150);
        assertThat(savedEntity.getReservedQuantity()).isEqualTo(30);
        assertThat(savedEntity.getVersion()).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("동시성 충돌 발생 시 OptimisticLockingFailureException을 던진다")
    void shouldThrowOptimisticLockingFailureExceptionOnConcurrentUpdate() {
        // given
        SkuId skuId = SkuId.generate();
        Inventory inventory = Inventory.createEmpty(skuId);
        
        when(inventoryJpaRepository.save(any(InventoryJpaEntity.class)))
                .thenThrow(new OptimisticLockException("Version conflict"));
        
        // when & then
        assertThatThrownBy(() -> adapter.save(inventory))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining("동시성 충돌이 발생했습니다")
                .hasMessageContaining(skuId.value());
    }
}