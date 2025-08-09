package com.commerce.product.domain.service.impl;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.model.inventory.Inventory;
import com.commerce.product.domain.model.inventory.SkuId;
import com.commerce.common.domain.model.Quantity;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAvailabilityServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private LockRepository lockRepository;
    
    private StockAvailabilityService stockAvailabilityService;
    
    private Executor ioExecutor;
    
    private ProductOption singleOption;
    private ProductOption bundleOption;

    @BeforeEach
    void setUp() {
        ioExecutor = Executors.newCachedThreadPool();
        stockAvailabilityService = new StockAvailabilityServiceImpl(inventoryRepository, productRepository, lockRepository, ioExecutor);
        
        singleOption = ProductOption.single(
                "Single Option",
                Money.of(BigDecimal.valueOf(10000), com.commerce.product.domain.model.Currency.KRW),
                SkuMapping.single("SKU001")
        );

        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 1);
        
        bundleOption = ProductOption.bundle(
                "Bundle Option", 
                Money.of(BigDecimal.valueOf(25000), com.commerce.product.domain.model.Currency.KRW),
                SkuMapping.bundle(bundleMapping)
        );
    }

    @Test
    @DisplayName("단일 옵션의 재고가 충분하면 true를 반환한다")
    void checkSingleOption_Available() {
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);

        boolean result = stockAvailabilityService.checkSingleOption("SKU001", 10);

        assertThat(result).isTrue();
        verify(inventoryRepository).getAvailableQuantity("SKU001");
    }

    @Test
    @DisplayName("단일 옵션의 재고가 부족하면 false를 반환한다")
    void checkSingleOption_NotAvailable() {
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(5);

        boolean result = stockAvailabilityService.checkSingleOption("SKU001", 10);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("묶음 옵션의 모든 SKU 재고가 충분하면 true를 반환한다")
    void checkBundleOption_AllAvailable() {
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);
        when(inventoryRepository.getAvailableQuantity("SKU002"))
                .thenReturn(50);

        boolean result = stockAvailabilityService.checkBundleOption(bundleOption, 10);

        assertThat(result).isTrue();
        verify(inventoryRepository).getAvailableQuantity("SKU001");
        verify(inventoryRepository).getAvailableQuantity("SKU002");
    }

    @Test
    @DisplayName("묶음 옵션의 일부 SKU 재고가 부족하면 false를 반환한다")
    void checkBundleOption_PartiallyNotAvailable() {
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);
        when(inventoryRepository.getAvailableQuantity("SKU002"))
                .thenReturn(5);

        boolean result = stockAvailabilityService.checkBundleOption(bundleOption, 10);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("여러 옵션의 재고를 병렬로 확인할 수 있다")
    void checkMultipleOptions() throws ExecutionException, InterruptedException {
        List<ProductOption> options = Arrays.asList(singleOption, bundleOption);
        Map<String, Integer> quantities = new HashMap<>();
        quantities.put("Single Option", 10);
        quantities.put("Bundle Option", 5);

        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);
        when(inventoryRepository.getAvailableQuantity("SKU002"))
                .thenReturn(50);

        CompletableFuture<Map<String, Boolean>> future = 
                stockAvailabilityService.checkMultipleOptions(options, quantities);
        Map<String, Boolean> result = future.get();

        assertThat(result).containsEntry("Single Option", true);
        assertThat(result).containsEntry("Bundle Option", true);
    }

    @Test
    @DisplayName("단일 SKU 재고 예약에 성공한다")
    void reserveStock_Success() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        Lock lock = new ReentrantLock();
        
        when(lockRepository.acquireLock("stock:SKU001", 5000L))
                .thenReturn(lock);
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);
        when(inventoryRepository.reserveStock("SKU001", 10, orderId))
                .thenReturn("RESERVATION001");

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveStock("SKU001", 10, orderId);
        Boolean result = future.get();

        assertThat(result).isTrue();
        verify(inventoryRepository).reserveStock("SKU001", 10, orderId);
        verify(lockRepository).releaseLock(lock);
    }

    @Test
    @DisplayName("재고 부족으로 예약에 실패한다")
    void reserveStock_InsufficientStock() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        Lock lock = new ReentrantLock();
        
        when(lockRepository.acquireLock("stock:SKU001", 5000L))
                .thenReturn(lock);
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(5);

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveStock("SKU001", 10, orderId);
        Boolean result = future.get();

        assertThat(result).isFalse();
        verify(inventoryRepository, never()).reserveStock(anyString(), anyInt(), anyString());
        verify(lockRepository).releaseLock(lock);
    }

    @Test
    @DisplayName("묶음 옵션 재고 예약에 성공한다 - Saga 패턴")
    void reserveBundleStock_Success() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        Lock lock1 = new ReentrantLock();
        Lock lock2 = new ReentrantLock();
        
        when(lockRepository.acquireLock("stock:SKU001", 5000L))
                .thenReturn(lock1);
        when(lockRepository.acquireLock("stock:SKU002", 5000L))
                .thenReturn(lock2);
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);
        when(inventoryRepository.getAvailableQuantity("SKU002"))
                .thenReturn(50);
        when(inventoryRepository.reserveStock("SKU001", 10, orderId))
                .thenReturn("RESERVATION001");
        when(inventoryRepository.reserveStock("SKU002", 5, orderId))
                .thenReturn("RESERVATION002");

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveBundleStock(bundleOption, 5, orderId);
        Boolean result = future.get();

        assertThat(result).isTrue();
        verify(inventoryRepository).reserveStock("SKU001", 10, orderId);
        verify(inventoryRepository).reserveStock("SKU002", 5, orderId);
        verify(lockRepository).releaseLock(lock1);
        verify(lockRepository).releaseLock(lock2);
    }

    @Test
    @DisplayName("묶음 옵션 예약 중 일부 실패 시 보상 트랜잭션이 실행된다")
    void reserveBundleStock_CompensatingTransaction() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        Lock lock1 = new ReentrantLock();
        Lock lock2 = new ReentrantLock();
        
        when(lockRepository.acquireLock("stock:SKU001", 5000L))
                .thenReturn(lock1);
        when(lockRepository.acquireLock("stock:SKU002", 5000L))
                .thenReturn(lock2);
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);
        when(inventoryRepository.getAvailableQuantity("SKU002"))
                .thenReturn(2);

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveBundleStock(bundleOption, 5, orderId);
        Boolean result = future.get();

        assertThat(result).isFalse();
        // 재고 체크 단계에서 실패하므로 예약이 발생하지 않음
        verify(inventoryRepository, never()).reserveStock(anyString(), anyInt(), anyString());
        verify(inventoryRepository, never()).releaseReservation(anyString());
        verify(lockRepository).releaseLock(lock1);
        verify(lockRepository).releaseLock(lock2);
    }

    @Test
    @DisplayName("묶음 옵션 예약 중 예외 발생 시 보상 트랜잭션이 실행된다")
    void reserveBundleStock_ExceptionCompensation() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        Lock lock1 = new ReentrantLock();
        Lock lock2 = new ReentrantLock();
        
        when(lockRepository.acquireLock("stock:SKU001", 5000L))
                .thenReturn(lock1);
        when(lockRepository.acquireLock("stock:SKU002", 5000L))
                .thenReturn(lock2);
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);
        when(inventoryRepository.getAvailableQuantity("SKU002"))
                .thenReturn(50);
        when(inventoryRepository.reserveStock("SKU001", 10, orderId))
                .thenReturn("RESERVATION001");
        when(inventoryRepository.reserveStock("SKU002", 5, orderId))
                .thenThrow(new RuntimeException("Database error"));

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveBundleStock(bundleOption, 5, orderId);
        Boolean result = future.get();

        assertThat(result).isFalse();
        verify(inventoryRepository).releaseReservation("RESERVATION001");
        verify(lockRepository).releaseLock(lock1);
        verify(lockRepository).releaseLock(lock2);
    }

    @Test
    @DisplayName("예약을 해제할 수 있다")
    void releaseReservation() {
        String reservationId = "RESERVATION001";
        
        doNothing().when(inventoryRepository).releaseReservation(reservationId);

        stockAvailabilityService.releaseReservation(reservationId);

        verify(inventoryRepository).releaseReservation(reservationId);
    }

    @Test
    @DisplayName("가용 재고량을 조회할 수 있다")
    void getAvailableQuantity() {
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(50);

        int quantity = stockAvailabilityService.getAvailableQuantity("SKU001");

        assertThat(quantity).isEqualTo(50);
    }

    @Test
    @DisplayName("묶음 옵션의 가용 재고량을 조회할 수 있다")
    void getBundleAvailableQuantity() {
        when(inventoryRepository.getAvailableQuantity("SKU001"))
                .thenReturn(100);
        when(inventoryRepository.getAvailableQuantity("SKU002"))
                .thenReturn(50);

        Map<String, Integer> quantities = 
                stockAvailabilityService.getBundleAvailableQuantity(bundleOption);

        assertThat(quantities).containsEntry("SKU001", 100);
        assertThat(quantities).containsEntry("SKU002", 50);
    }

    @Test
    @DisplayName("락 획득 실패 시 예약이 실패한다")
    void reserveStock_LockTimeout() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        
        when(lockRepository.acquireLock("stock:SKU001", 5000L))
                .thenReturn(null);

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveStock("SKU001", 10, orderId);
        Boolean result = future.get();

        assertThat(result).isFalse();
        verify(inventoryRepository, never()).getAvailableQuantity(anyString());
        verify(inventoryRepository, never()).reserveStock(anyString(), anyInt(), anyString());
    }
    
    @Test
    @DisplayName("checkProductOptionAvailability - 단일 옵션의 재고가 충분한 경우")
    void checkProductOptionAvailability_SingleOption_Available() throws ExecutionException, InterruptedException {
        String optionId = "OPTION001";
        SkuId skuId = SkuId.of("SKU001");
        Inventory inventory = mock(Inventory.class);
        Quantity quantity = mock(Quantity.class);
        
        when(productRepository.findOptionById(optionId))
                .thenReturn(Optional.of(singleOption));
        when(inventoryRepository.findBySkuId(skuId))
                .thenReturn(Optional.of(inventory));
        when(inventory.getAvailableQuantity()).thenReturn(quantity);
        when(quantity.value()).thenReturn(50);
        
        CompletableFuture<AvailabilityResult> future = 
                stockAvailabilityService.checkProductOptionAvailability(optionId);
        AvailabilityResult result = future.get();
        
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.availableQuantity()).isEqualTo(50);
    }
    
    @Test
    @DisplayName("checkProductOptionAvailability - 옵션이 없는 경우")
    void checkProductOptionAvailability_OptionNotFound() throws ExecutionException, InterruptedException {
        String optionId = "OPTION_NOT_FOUND";
        
        when(productRepository.findOptionById(optionId))
                .thenReturn(Optional.empty());
        
        CompletableFuture<AvailabilityResult> future = 
                stockAvailabilityService.checkProductOptionAvailability(optionId);
        AvailabilityResult result = future.get();
        
        assertThat(result.isAvailable()).isFalse();
    }
    
    @Test
    @DisplayName("checkBundleAvailability - 묶음 옵션의 모든 SKU 재고가 충분한 경우")
    void checkBundleAvailability_AllAvailable() throws ExecutionException, InterruptedException {
        SkuMapping skuMapping = SkuMapping.bundle(Map.of("SKU001", 2, "SKU002", 1));
        Lock lock = new ReentrantLock();
        
        Inventory inventory1 = mock(Inventory.class);
        Inventory inventory2 = mock(Inventory.class);
        Quantity quantity1 = mock(Quantity.class);
        Quantity quantity2 = mock(Quantity.class);
        
        when(lockRepository.acquireLock(anyString(), eq(5000L)))
                .thenReturn(lock);
        when(inventoryRepository.findBySkuIds(anyList()))
                .thenReturn(Map.of(
                    SkuId.of("SKU001"), inventory1,
                    SkuId.of("SKU002"), inventory2
                ));
        when(inventory1.getAvailableQuantity()).thenReturn(quantity1);
        when(inventory2.getAvailableQuantity()).thenReturn(quantity2);
        when(quantity1.value()).thenReturn(10);
        when(quantity2.value()).thenReturn(5);
        
        CompletableFuture<BundleAvailabilityResult> future = 
                stockAvailabilityService.checkBundleAvailability(skuMapping);
        BundleAvailabilityResult result = future.get();
        
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.availableSets()).isEqualTo(5); // min(10/2, 5/1) = min(5, 5) = 5
        verify(lockRepository).releaseLock(lock);
    }
    
    @Test
    @DisplayName("checkBundleAvailability - 일부 SKU 재고가 부족한 경우")
    void checkBundleAvailability_PartiallyUnavailable() throws ExecutionException, InterruptedException {
        SkuMapping skuMapping = SkuMapping.bundle(Map.of("SKU001", 2, "SKU002", 1));
        Lock lock = new ReentrantLock();
        
        Inventory inventory1 = mock(Inventory.class);
        Quantity quantity1 = mock(Quantity.class);
        
        when(lockRepository.acquireLock(anyString(), eq(5000L)))
                .thenReturn(lock);
        when(inventoryRepository.findBySkuIds(anyList()))
                .thenReturn(Map.of(
                    SkuId.of("SKU001"), inventory1
                    // SKU002 is missing
                ));
        when(inventory1.getAvailableQuantity()).thenReturn(quantity1);
        when(quantity1.value()).thenReturn(10);
        
        CompletableFuture<BundleAvailabilityResult> future = 
                stockAvailabilityService.checkBundleAvailability(skuMapping);
        BundleAvailabilityResult result = future.get();
        
        assertThat(result.isAvailable()).isFalse();
        assertThat(result.availableSets()).isEqualTo(0);
        verify(lockRepository).releaseLock(lock);
    }
}