package com.commerce.product.domain.service.impl;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.exception.LockAcquisitionException;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.SagaRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    
    @Mock
    private SagaRepository sagaRepository;
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    private StockAvailabilityService stockAvailabilityService;
    
    private ProductOption singleOption;
    private ProductOption bundleOption;

    @BeforeEach
    void setUp() {
        stockAvailabilityService = new StockAvailabilityServiceImpl(inventoryRepository, productRepository, lockRepository, sagaRepository, eventPublisher);
        
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
        DistributedLock lock = new DistributedLock("stock:SKU001", "lock-123", Instant.now(), Duration.ofSeconds(30));
        
        when(lockRepository.acquireLock(eq("stock:SKU001"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock));
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
        DistributedLock lock = new DistributedLock("stock:SKU001", "lock-123", Instant.now(), Duration.ofSeconds(30));
        
        when(lockRepository.acquireLock(eq("stock:SKU001"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock));
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
        DistributedLock lock1 = new DistributedLock("stock:SKU001", "lock-123", Instant.now(), Duration.ofSeconds(30));
        DistributedLock lock2 = new DistributedLock("stock:SKU002", "lock-456", Instant.now(), Duration.ofSeconds(30));
        
        when(lockRepository.acquireLock(eq("stock:SKU001"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock1));
        when(lockRepository.acquireLock(eq("stock:SKU002"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock2));
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
        verify(lockRepository).releaseLock(lock2);
        verify(lockRepository).releaseLock(lock1);
    }

    @Test
    @DisplayName("묶음 옵션 예약 중 일부 실패 시 보상 트랜잭션이 실행된다")
    void reserveBundleStock_CompensatingTransaction() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        DistributedLock lock1 = new DistributedLock("stock:SKU001", "lock-123", Instant.now(), Duration.ofSeconds(30));
        DistributedLock lock2 = new DistributedLock("stock:SKU002", "lock-456", Instant.now(), Duration.ofSeconds(30));
        
        when(lockRepository.acquireLock(eq("stock:SKU001"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock1));
        when(lockRepository.acquireLock(eq("stock:SKU002"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock2));
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
        verify(lockRepository).releaseLock(lock2);
        verify(lockRepository).releaseLock(lock1);
    }

    @Test
    @DisplayName("묶음 옵션 예약 중 예외 발생 시 보상 트랜잭션이 실행된다")
    void reserveBundleStock_ExceptionCompensation() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        DistributedLock lock1 = new DistributedLock("stock:SKU001", "lock-123", Instant.now(), Duration.ofSeconds(30));
        DistributedLock lock2 = new DistributedLock("stock:SKU002", "lock-456", Instant.now(), Duration.ofSeconds(30));
        
        when(lockRepository.acquireLock(eq("stock:SKU001"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock1));
        when(lockRepository.acquireLock(eq("stock:SKU002"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock2));
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
        verify(lockRepository).releaseLock(lock2);
        verify(lockRepository).releaseLock(lock1);
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
    @DisplayName("묶음 옵션 예약 중 락 획득 실패 시 false를 반환한다")
    void reserveBundleStock_LockAcquisitionFails() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        DistributedLock lock1 = new DistributedLock("stock:SKU001", "lock-123", Instant.now(), Duration.ofSeconds(30));
        
        when(lockRepository.acquireLock(eq("stock:SKU001"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock1));
        when(lockRepository.acquireLock(eq("stock:SKU002"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.empty());

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveBundleStock(bundleOption, 5, orderId);
        Boolean result = future.get();

        assertThat(result).isFalse();
        verify(lockRepository).releaseLock(lock1);
        verify(inventoryRepository, never()).reserveStock(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("락 획득 실패 시 예외가 발생한다")
    void reserveStock_LockTimeout() {
        String orderId = "ORDER001";
        
        when(lockRepository.acquireLock(eq("stock:SKU001"), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.empty());

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveStock("SKU001", 10, orderId);
        
        assertThatThrownBy(() -> future.get())
                .hasCauseInstanceOf(LockAcquisitionException.class);
        
        verify(inventoryRepository, never()).getAvailableQuantity(anyString());
        verify(inventoryRepository, never()).reserveStock(anyString(), anyInt(), anyString());
    }
}