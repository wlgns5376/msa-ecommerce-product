package com.commerce.product.domain.service.impl;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockAvailabilityServiceConcurrencyTest {
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private LockRepository lockRepository;
    
    private StockAvailabilityService stockAvailabilityService;
    
    private final Map<String, AtomicInteger> stockMap = new ConcurrentHashMap<>();
    private final Map<String, DistributedLock> activeLocks = new ConcurrentHashMap<>();
    
    @BeforeEach
    void setUp() {
        stockAvailabilityService = new StockAvailabilityServiceImpl(inventoryRepository, lockRepository);
        
        // 초기 재고 설정
        stockMap.put("SKU001", new AtomicInteger(100));
        stockMap.put("SKU002", new AtomicInteger(50));
        
        // Mock 설정
        setupMocks();
    }
    
    private void setupMocks() {
        // 재고 조회 Mock
        when(inventoryRepository.getAvailableQuantity(anyString()))
            .thenAnswer(invocation -> {
                String skuId = invocation.getArgument(0);
                return stockMap.getOrDefault(skuId, new AtomicInteger(0)).get();
            });
        
        // 재고 예약 Mock
        when(inventoryRepository.reserveStock(anyString(), anyInt(), anyString()))
            .thenAnswer(invocation -> {
                String skuId = invocation.getArgument(0);
                int quantity = invocation.getArgument(1);
                AtomicInteger stock = stockMap.get(skuId);
                
                if (stock != null && stock.get() >= quantity) {
                    stock.addAndGet(-quantity);
                    return "RESERVATION-" + UUID.randomUUID();
                }
                throw new RuntimeException("Insufficient stock");
            });
        
        // 분산 락 Mock - 실제 동시성 제어 시뮬레이션
        when(lockRepository.acquireLock(anyString(), any(Duration.class), any(Duration.class)))
            .thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Duration leaseDuration = invocation.getArgument(1);
                Duration waitTimeout = invocation.getArgument(2);
                
                long startTime = System.currentTimeMillis();
                long waitTimeoutMillis = waitTimeout.toMillis();
                
                while (System.currentTimeMillis() - startTime < waitTimeoutMillis) {
                    DistributedLock newLock = new DistributedLock(
                        key.replace("stock:", ""), 
                        UUID.randomUUID().toString(), 
                        Instant.now(), 
                        leaseDuration
                    );
                    
                    DistributedLock existingLock = activeLocks.putIfAbsent(key, newLock);
                    if (existingLock == null) {
                        return Optional.of(newLock);
                    }
                    
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return Optional.empty();
                    }
                }
                
                return Optional.empty();
            });
        
        // 락 해제 Mock
        when(lockRepository.releaseLock(any(DistributedLock.class)))
            .thenAnswer(invocation -> {
                DistributedLock lock = invocation.getArgument(0);
                return activeLocks.remove("stock:" + lock.key(), lock);
            });
    }
    
    @Test
    @DisplayName("동시에 여러 요청이 들어와도 재고가 정확하게 차감된다")
    void concurrentStockReservation() throws InterruptedException {
        // Given
        int threadCount = 10;
        int quantityPerThread = 5;
        String skuId = "SKU001";
        int initialStock = stockMap.get(skuId).get();
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        // When
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final String orderId = "ORDER-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CompletableFuture<Boolean> future = 
                        stockAvailabilityService.reserveStock(skuId, quantityPerThread, orderId);
                    
                    Boolean result = future.get(5, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(result)) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        
        // Then
        int expectedSuccessCount = initialStock / quantityPerThread;
        int finalStock = stockMap.get(skuId).get();
        
        assertThat(successCount.get()).isLessThanOrEqualTo(expectedSuccessCount);
        assertThat(finalStock).isEqualTo(initialStock - (successCount.get() * quantityPerThread));
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("묶음 상품의 동시 예약 시 모든 SKU가 원자적으로 처리된다")
    void concurrentBundleReservation() throws InterruptedException {
        // Given
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 2);
        bundleMapping.put("SKU002", 1);
        
        ProductOption bundleOption = ProductOption.bundle(
            "Bundle Option",
            Money.of(BigDecimal.valueOf(30000), com.commerce.product.domain.model.Currency.KRW),
            SkuMapping.bundle(bundleMapping)
        );
        
        int threadCount = 5;
        int quantityPerThread = 10;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        int initialStock1 = stockMap.get("SKU001").get();
        int initialStock2 = stockMap.get("SKU002").get();
        
        // When
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final String orderId = "BUNDLE-ORDER-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CompletableFuture<Boolean> future = 
                        stockAvailabilityService.reserveBundleStock(bundleOption, quantityPerThread, orderId);
                    
                    Boolean result = future.get(5, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(result)) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 실패 케이스 - 예상됨
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        
        // Then
        int finalStock1 = stockMap.get("SKU001").get();
        int finalStock2 = stockMap.get("SKU002").get();
        
        // 번들 예약이 성공한 횟수만큼 정확히 차감되어야 함
        int expectedStock1 = initialStock1 - (successCount.get() * quantityPerThread * 2);
        int expectedStock2 = initialStock2 - (successCount.get() * quantityPerThread * 1);
        
        assertThat(finalStock1).isEqualTo(expectedStock1);
        assertThat(finalStock2).isEqualTo(expectedStock2);
        
        // 두 SKU의 재고 비율이 유지되어야 함
        if (successCount.get() > 0) {
            int consumedStock1 = initialStock1 - finalStock1;
            int consumedStock2 = initialStock2 - finalStock2;
            assertThat(consumedStock1).isEqualTo(consumedStock2 * 2);
        }
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("락 타임아웃 시 적절히 실패 처리된다")
    void lockTimeoutHandling() throws InterruptedException, ExecutionException {
        // Given
        String skuId = "SKU001";
        int threadCount = 5;
        AtomicInteger lockAttempts = new AtomicInteger(0);
        
        // 첫 번째 스레드만 성공, 나머지는 타임아웃
        when(lockRepository.acquireLock(eq("stock:" + skuId), any(Duration.class), any(Duration.class)))
            .thenAnswer(invocation -> {
                int attempt = lockAttempts.incrementAndGet();
                if (attempt == 1) {
                    // 첫 번째 스레드만 락 획듍 성공
                    DistributedLock lock = new DistributedLock(
                        skuId, 
                        UUID.randomUUID().toString(), 
                        Instant.now(), 
                        Duration.ofSeconds(10)
                    );
                    activeLocks.put("stock:" + skuId, lock);
                    return Optional.of(lock);
                } else {
                    // 나머지는 락이 이미 있으므로 실패
                    return Optional.empty();
                }
            });
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        
        // When
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final String orderId = "TIMEOUT-ORDER-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // 재고 예약 시도
                    stockAvailabilityService.reserveStock(skuId, 5, orderId).get(3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    if (e.getCause() != null && e.getCause().getMessage().contains("Unable to acquire lock")) {
                        timeoutCount.incrementAndGet();
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        
        // Then - 첫 번째 스레드를 제외한 나머지는 모두 락 획듍 실패
        assertThat(timeoutCount.get()).isEqualTo(threadCount - 1);
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("동시 요청 중 일부가 재고 부족으로 실패해도 데이터 정합성이 유지된다")
    void partialFailureMaintainsConsistency() throws InterruptedException {
        // Given
        String skuId = "SKU002";
        stockMap.get(skuId).set(30); // 제한된 재고
        
        int threadCount = 10;
        int quantityPerThread = 10;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        // When
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final String orderId = "PARTIAL-ORDER-" + i;
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    startLatch.await();
                    return stockAvailabilityService.reserveStock(skuId, quantityPerThread, orderId)
                        .get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return false;
                } finally {
                    endLatch.countDown();
                }
            }, executor);
            
            futures.add(future);
        }
        
        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        
        // Then
        long successCount = futures.stream()
            .map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    return false;
                }
            })
            .filter(Boolean::booleanValue)
            .count();
        
        assertThat(successCount).isEqualTo(3); // 30 / 10 = 3
        assertThat(stockMap.get(skuId).get()).isEqualTo(0);
        
        executor.shutdown();
    }
}