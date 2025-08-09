package com.commerce.product.domain.service.impl;

import com.commerce.product.domain.model.inventory.Inventory;
import com.commerce.product.domain.model.inventory.Quantity;
import com.commerce.product.domain.model.inventory.SkuId;
import com.commerce.product.domain.model.SkuMapping;
import com.commerce.product.domain.repository.InventoryRepositoryV2;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.StockAvailabilityServiceV2;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAvailabilityServiceV2ImplTest {

    @Mock
    private InventoryRepositoryV2 inventoryRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private LockRepository lockRepository;
    
    private StockAvailabilityServiceV2 stockAvailabilityService;
    
    @BeforeEach
    void setUp() {
        stockAvailabilityService = new StockAvailabilityServiceV2Impl(
            inventoryRepository, 
            productRepository, 
            lockRepository
        );
    }

    @Test
    @DisplayName("묶음 옵션의 모든 SKU 재고가 충분하면 가용 세트 수를 계산한다")
    void checkBundleAvailability_AllAvailable() throws ExecutionException, InterruptedException {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 3);
        SkuMapping skuMapping = SkuMapping.bundle(bundleMapping);
        
        Lock lock = new ReentrantLock();
        when(lockRepository.acquireLock(any(), anyLong())).thenReturn(lock);
        
        // SKU001: 100개 (2개씩 필요하므로 50세트 가능)
        // SKU002: 90개 (3개씩 필요하므로 30세트 가능)
        Inventory inventory1 = mock(Inventory.class);
        when(inventory1.getAvailableQuantity()).thenReturn(Quantity.of(100));
        
        Inventory inventory2 = mock(Inventory.class);
        when(inventory2.getAvailableQuantity()).thenReturn(Quantity.of(90));
        
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(SkuId.of("SKU001"), inventory1);
        inventoryMap.put(SkuId.of("SKU002"), inventory2);
        when(inventoryRepository.findBySkuIds(any(List.class))).thenReturn(inventoryMap);

        // When
        CompletableFuture<BundleAvailabilityResult> future = 
            stockAvailabilityService.checkBundleAvailability(skuMapping);
        BundleAvailabilityResult result = future.get();

        // Then
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.availableSets()).isEqualTo(30); // 최소값인 30세트
        assertThat(result.details()).hasSize(2);
        
        // SKU001 상세 정보 확인
        var sku1Detail = result.details().stream()
            .filter(d -> d.skuId().equals("SKU001"))
            .findFirst()
            .orElseThrow();
        assertThat(sku1Detail.requiredQuantity()).isEqualTo(2);
        assertThat(sku1Detail.availableQuantity()).isEqualTo(100);
        assertThat(sku1Detail.availableSets()).isEqualTo(50);
        
        // SKU002 상세 정보 확인
        var sku2Detail = result.details().stream()
            .filter(d -> d.skuId().equals("SKU002"))
            .findFirst()
            .orElseThrow();
        assertThat(sku2Detail.requiredQuantity()).isEqualTo(3);
        assertThat(sku2Detail.availableQuantity()).isEqualTo(90);
        assertThat(sku2Detail.availableSets()).isEqualTo(30);
        
        verify(lockRepository).releaseLock(lock);
    }

    @Test
    @DisplayName("묶음 옵션의 일부 SKU 재고가 부족하면 가용하지 않음을 반환한다")
    void checkBundleAvailability_PartiallyUnavailable() throws ExecutionException, InterruptedException {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 3);
        SkuMapping skuMapping = SkuMapping.bundle(bundleMapping);
        
        Lock lock = new ReentrantLock();
        when(lockRepository.acquireLock(any(), anyLong())).thenReturn(lock);
        
        // SKU001: 100개 (2개씩 필요하므로 50세트 가능)
        // SKU002: 2개 (3개씩 필요하므로 0세트 가능)
        Inventory inventory1 = mock(Inventory.class);
        when(inventory1.getAvailableQuantity()).thenReturn(Quantity.of(100));
        
        Inventory inventory2 = mock(Inventory.class);
        when(inventory2.getAvailableQuantity()).thenReturn(Quantity.of(2));
        
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(SkuId.of("SKU001"), inventory1);
        inventoryMap.put(SkuId.of("SKU002"), inventory2);
        when(inventoryRepository.findBySkuIds(any(List.class))).thenReturn(inventoryMap);

        // When
        CompletableFuture<BundleAvailabilityResult> future = 
            stockAvailabilityService.checkBundleAvailability(skuMapping);
        BundleAvailabilityResult result = future.get();

        // Then
        assertThat(result.isAvailable()).isFalse();
        assertThat(result.availableSets()).isEqualTo(0);
        assertThat(result.details()).hasSize(2);
        
        // SKU002의 가용 세트가 0임을 확인
        var sku2Detail = result.details().stream()
            .filter(d -> d.skuId().equals("SKU002"))
            .findFirst()
            .orElseThrow();
        assertThat(sku2Detail.availableSets()).isEqualTo(0);
        
        verify(lockRepository).releaseLock(lock);
    }

    @Test
    @DisplayName("묶음 옵션 재고 확인 시 락 획득에 실패하면 예외가 발생한다")
    void checkBundleAvailability_LockAcquisitionFailure() {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 3);
        SkuMapping skuMapping = SkuMapping.bundle(bundleMapping);
        
        when(lockRepository.acquireLock(any(), anyLong())).thenReturn(null);

        // When & Then
        CompletableFuture<BundleAvailabilityResult> future = 
            stockAvailabilityService.checkBundleAvailability(skuMapping);
        
        org.assertj.core.api.Assertions.assertThatThrownBy(future::get)
            .isInstanceOf(java.util.concurrent.ExecutionException.class)
            .hasCauseInstanceOf(com.commerce.product.domain.exception.LockAcquisitionException.class);
    }

    @Test
    @DisplayName("묶음 옵션 재고 확인 시 SKU 정렬을 통해 데드락을 방지한다")
    void checkBundleAvailability_PreventDeadlock() throws ExecutionException, InterruptedException {
        // Given - 역순으로 SKU 추가
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU003", 1);
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 3);
        SkuMapping skuMapping = SkuMapping.bundle(bundleMapping);
        
        Lock lock = new ReentrantLock();
        when(lockRepository.acquireLock(any(), anyLong())).thenReturn(lock);
        
        Inventory inventory1 = mock(Inventory.class);
        when(inventory1.getAvailableQuantity()).thenReturn(Quantity.of(100));
        
        Inventory inventory2 = mock(Inventory.class);
        when(inventory2.getAvailableQuantity()).thenReturn(Quantity.of(90));
        
        Inventory inventory3 = mock(Inventory.class);
        when(inventory3.getAvailableQuantity()).thenReturn(Quantity.of(50));
        
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(SkuId.of("SKU001"), inventory1);
        inventoryMap.put(SkuId.of("SKU002"), inventory2);
        inventoryMap.put(SkuId.of("SKU003"), inventory3);
        when(inventoryRepository.findBySkuIds(any(List.class))).thenReturn(inventoryMap);

        // When
        CompletableFuture<BundleAvailabilityResult> future = 
            stockAvailabilityService.checkBundleAvailability(skuMapping);
        BundleAvailabilityResult result = future.get();

        // Then - 락 키가 정렬된 순서로 생성되었는지 확인
        verify(lockRepository).acquireLock(eq("bundle-stock-check:SKU001:SKU002:SKU003"), anyLong());
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 SKU에 대해 재고 확인 시 가용하지 않음을 반환한다")
    void checkBundleAvailability_NonExistentSku() throws ExecutionException, InterruptedException {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("NON_EXISTENT", 1);
        SkuMapping skuMapping = SkuMapping.bundle(bundleMapping);
        
        Lock lock = new ReentrantLock();
        when(lockRepository.acquireLock(any(), anyLong())).thenReturn(lock);
        
        Inventory inventory1 = mock(Inventory.class);
        when(inventory1.getAvailableQuantity()).thenReturn(Quantity.of(100));
        
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(SkuId.of("SKU001"), inventory1);
        // NON_EXISTENT는 map에 없음
        when(inventoryRepository.findBySkuIds(any(List.class))).thenReturn(inventoryMap);

        // When
        CompletableFuture<BundleAvailabilityResult> future = 
            stockAvailabilityService.checkBundleAvailability(skuMapping);
        BundleAvailabilityResult result = future.get();

        // Then
        assertThat(result.isAvailable()).isFalse();
        assertThat(result.availableSets()).isEqualTo(0);
        
        // 존재하지 않는 SKU의 상세 정보 확인
        var nonExistentDetail = result.details().stream()
            .filter(d -> d.skuId().equals("NON_EXISTENT"))
            .findFirst()
            .orElseThrow();
        assertThat(nonExistentDetail.availableQuantity()).isEqualTo(0);
        assertThat(nonExistentDetail.availableSets()).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 스레드에서 동시에 묶음 옵션 재고를 확인해도 안전하다")
    void checkBundleAvailability_ConcurrentAccess() throws InterruptedException {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 3);
        SkuMapping skuMapping = SkuMapping.bundle(bundleMapping);
        
        Lock lock = new ReentrantLock();
        when(lockRepository.acquireLock(any(), anyLong())).thenReturn(lock);
        
        Inventory inventory1 = mock(Inventory.class);
        when(inventory1.getAvailableQuantity()).thenReturn(Quantity.of(100));
        
        Inventory inventory2 = mock(Inventory.class);
        when(inventory2.getAvailableQuantity()).thenReturn(Quantity.of(90));
        
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(SkuId.of("SKU001"), inventory1);
        inventoryMap.put(SkuId.of("SKU002"), inventory2);
        when(inventoryRepository.findBySkuIds(any(List.class))).thenReturn(inventoryMap);

        // When - 여러 스레드에서 동시 실행
        int threadCount = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.List<CompletableFuture<BundleAvailabilityResult>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return stockAvailabilityService.checkBundleAvailability(skuMapping).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // Then - 모든 결과가 동일해야 함
        for (CompletableFuture<BundleAvailabilityResult> future : futures) {
            assertThat(future.join().isAvailable()).isTrue();
        }
        
        // 락 획득/해제가 스레드 수만큼 호출되었는지 확인
        verify(lockRepository, times(threadCount)).acquireLock(any(), anyLong());
        verify(lockRepository, times(threadCount)).releaseLock(any());
    }
}