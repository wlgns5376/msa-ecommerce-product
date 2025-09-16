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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
        // 테스트용 배치 크기 설정
        ReflectionTestUtils.setField(adapter, "batchSize", 1000);
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
    
    @Test
    @DisplayName("여러 재고를 한 번에 저장할 수 있다")
    void shouldSaveAllInventories() {
        // given
        SkuId skuId1 = SkuId.generate();
        SkuId skuId2 = SkuId.generate();
        
        Inventory inventory1 = Inventory.createWithInitialStock(skuId1, Quantity.of(100));
        inventory1.reserve(Quantity.of(20), "ORDER-2024-001", 3600);
        
        Inventory inventory2 = Inventory.createWithInitialStock(skuId2, Quantity.of(50));
        inventory2.reserve(Quantity.of(10), "ORDER-2024-002", 3600);
        
        List<Inventory> inventories = Arrays.asList(inventory1, inventory2);
        
        // when
        adapter.saveAll(inventories);
        
        // then
        ArgumentCaptor<List<InventoryJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(inventoryJpaRepository).saveAll(captor.capture());
        
        List<InventoryJpaEntity> savedEntities = captor.getValue();
        assertThat(savedEntities).hasSize(2);
        
        InventoryJpaEntity savedEntity1 = savedEntities.stream()
            .filter(e -> e.getSkuId().equals(skuId1.value()))
            .findFirst()
            .orElseThrow();
        assertThat(savedEntity1.getTotalQuantity()).isEqualTo(100);
        assertThat(savedEntity1.getReservedQuantity()).isEqualTo(20);
        
        InventoryJpaEntity savedEntity2 = savedEntities.stream()
            .filter(e -> e.getSkuId().equals(skuId2.value()))
            .findFirst()
            .orElseThrow();
        assertThat(savedEntity2.getTotalQuantity()).isEqualTo(50);
        assertThat(savedEntity2.getReservedQuantity()).isEqualTo(10);
    }
    
    @Test
    @DisplayName("saveAll 실행 중 동시성 충돌 발생 시 OptimisticLockingFailureException을 던진다")
    void shouldThrowOptimisticLockingFailureExceptionOnConcurrentBatchUpdate() {
        // given
        SkuId skuId1 = SkuId.generate();
        SkuId skuId2 = SkuId.generate();
        
        Inventory inventory1 = Inventory.createWithInitialStock(skuId1, Quantity.of(100));
        Inventory inventory2 = Inventory.createWithInitialStock(skuId2, Quantity.of(50));
        
        List<Inventory> inventories = Arrays.asList(inventory1, inventory2);
        
        when(inventoryJpaRepository.saveAll(anyList()))
                .thenThrow(new OptimisticLockException("Version conflict"));
        
        // when & then
        assertThatThrownBy(() -> adapter.saveAll(inventories))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining("동시성 충돌이 발생했습니다");
    }
    
    @Test
    @DisplayName("여러 SKU ID로 재고를 조회할 수 있다")
    void shouldLoadAllInventoriesByIds() {
        // given
        SkuId skuId1 = SkuId.generate();
        SkuId skuId2 = SkuId.generate();
        SkuId skuId3 = SkuId.generate();
        
        InventoryJpaEntity entity1 = InventoryJpaEntity.builder()
                .skuId(skuId1.value())
                .totalQuantity(100)
                .reservedQuantity(20)
                .version(1L)
                .build();
                
        InventoryJpaEntity entity2 = InventoryJpaEntity.builder()
                .skuId(skuId2.value())
                .totalQuantity(200)
                .reservedQuantity(40)
                .version(2L)
                .build();
        
        List<SkuId> skuIds = Arrays.asList(skuId1, skuId2, skuId3);
        List<String> skuIdValues = Arrays.asList(skuId1.value(), skuId2.value(), skuId3.value());
        
        when(inventoryJpaRepository.findAllById(skuIdValues))
                .thenReturn(Arrays.asList(entity1, entity2));
        
        // when
        Map<SkuId, Inventory> result = adapter.loadAllByIds(skuIds);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(skuId1)).isNotNull();
        assertThat(result.get(skuId1).getTotalQuantity()).isEqualTo(Quantity.of(100));
        assertThat(result.get(skuId2)).isNotNull();
        assertThat(result.get(skuId2).getTotalQuantity()).isEqualTo(Quantity.of(200));
        assertThat(result.get(skuId3)).isNull();
    }
    
    @Test
    @DisplayName("1000개 이상의 SKU ID로 조회 시 배치로 나누어 처리한다")
    void shouldProcessInBatchesWhenLoadingMoreThan1000Ids() {
        // given
        List<SkuId> skuIds = IntStream.range(0, 2500)
                .mapToObj(i -> SkuId.of("SKU-" + i))
                .collect(Collectors.toList());
        
        // 첫 번째 배치 (0-999)
        List<String> firstBatch = IntStream.range(0, 1000)
                .mapToObj(i -> "SKU-" + i)
                .collect(Collectors.toList());
        List<InventoryJpaEntity> firstBatchEntities = IntStream.range(0, 1000)
                .mapToObj(i -> InventoryJpaEntity.builder()
                        .skuId("SKU-" + i)
                        .totalQuantity(100 + i)
                        .reservedQuantity(10)
                        .version(1L)
                        .build())
                .collect(Collectors.toList());
        
        // 두 번째 배치 (1000-1999)
        List<String> secondBatch = IntStream.range(1000, 2000)
                .mapToObj(i -> "SKU-" + i)
                .collect(Collectors.toList());
        List<InventoryJpaEntity> secondBatchEntities = IntStream.range(1000, 2000)
                .mapToObj(i -> InventoryJpaEntity.builder()
                        .skuId("SKU-" + i)
                        .totalQuantity(100 + i)
                        .reservedQuantity(10)
                        .version(1L)
                        .build())
                .collect(Collectors.toList());
        
        // 세 번째 배치 (2000-2499)
        List<String> thirdBatch = IntStream.range(2000, 2500)
                .mapToObj(i -> "SKU-" + i)
                .collect(Collectors.toList());
        List<InventoryJpaEntity> thirdBatchEntities = IntStream.range(2000, 2500)
                .mapToObj(i -> InventoryJpaEntity.builder()
                        .skuId("SKU-" + i)
                        .totalQuantity(100 + i)
                        .reservedQuantity(10)
                        .version(1L)
                        .build())
                .collect(Collectors.toList());
        
        when(inventoryJpaRepository.findAllById(firstBatch))
                .thenReturn(firstBatchEntities);
        when(inventoryJpaRepository.findAllById(secondBatch))
                .thenReturn(secondBatchEntities);
        when(inventoryJpaRepository.findAllById(thirdBatch))
                .thenReturn(thirdBatchEntities);
        
        // when
        Map<SkuId, Inventory> result = adapter.loadAllByIds(skuIds);
        
        // then
        assertThat(result).hasSize(2500);
        verify(inventoryJpaRepository, times(3)).findAllById(anyList());
        
        // 각 배치가 올바른 크기로 호출되었는지 확인
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(inventoryJpaRepository, times(3)).findAllById(captor.capture());
        List<List<String>> allBatches = captor.getAllValues();
        
        assertThat(allBatches.get(0)).hasSize(1000);
        assertThat(allBatches.get(1)).hasSize(1000);
        assertThat(allBatches.get(2)).hasSize(500);
        
        // 결과 검증
        assertThat(result.get(SkuId.of("SKU-0")).getTotalQuantity()).isEqualTo(Quantity.of(100));
        assertThat(result.get(SkuId.of("SKU-999")).getTotalQuantity()).isEqualTo(Quantity.of(1099));
        assertThat(result.get(SkuId.of("SKU-1000")).getTotalQuantity()).isEqualTo(Quantity.of(1100));
        assertThat(result.get(SkuId.of("SKU-2499")).getTotalQuantity()).isEqualTo(Quantity.of(2599));
    }
    
    @Test
    @DisplayName("빈 리스트로 조회 시 빈 맵을 반환한다")
    void shouldReturnEmptyMapWhenLoadingWithEmptyList() {
        // when
        Map<SkuId, Inventory> result = adapter.loadAllByIds(new ArrayList<>());
        
        // then
        assertThat(result).isEmpty();
        verify(inventoryJpaRepository, never()).findAllById(anyList());
    }
    
    @Test
    @DisplayName("null 리스트로 조회 시 빈 맵을 반환한다")
    void shouldReturnEmptyMapWhenLoadingWithNullList() {
        // when
        Map<SkuId, Inventory> result = adapter.loadAllByIds(null);
        
        // then
        assertThat(result).isEmpty();
        verify(inventoryJpaRepository, never()).findAllById(anyList());
    }
}