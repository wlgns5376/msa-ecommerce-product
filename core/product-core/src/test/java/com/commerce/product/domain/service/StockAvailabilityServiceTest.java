package com.commerce.product.domain.service;

import com.commerce.product.domain.model.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAvailabilityServiceTest {

    @Mock
    private StockAvailabilityService stockAvailabilityService;

    private ProductOption singleOption;
    private ProductOption bundleOption;

    @BeforeEach
    void setUp() {
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
    @DisplayName("단일 옵션의 재고를 확인할 수 있다")
    void checkSingleOption() {
        when(stockAvailabilityService.checkSingleOption("SKU001", 10))
                .thenReturn(true);

        boolean available = stockAvailabilityService.checkSingleOption("SKU001", 10);

        assertThat(available).isTrue();
        verify(stockAvailabilityService).checkSingleOption("SKU001", 10);
    }

    @Test
    @DisplayName("단일 옵션의 재고가 부족하면 false를 반환한다")
    void checkSingleOption_NotAvailable() {
        when(stockAvailabilityService.checkSingleOption("SKU001", 100))
                .thenReturn(false);

        boolean available = stockAvailabilityService.checkSingleOption("SKU001", 100);

        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("묶음 옵션의 재고를 확인할 수 있다")
    void checkBundleOption() {
        when(stockAvailabilityService.checkBundleOption(bundleOption, 5))
                .thenReturn(true);

        boolean available = stockAvailabilityService.checkBundleOption(bundleOption, 5);

        assertThat(available).isTrue();
        verify(stockAvailabilityService).checkBundleOption(bundleOption, 5);
    }

    @Test
    @DisplayName("묶음 옵션의 일부 SKU 재고가 부족하면 false를 반환한다")
    void checkBundleOption_PartiallyNotAvailable() {
        when(stockAvailabilityService.checkBundleOption(bundleOption, 50))
                .thenReturn(false);

        boolean available = stockAvailabilityService.checkBundleOption(bundleOption, 50);

        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("여러 옵션의 재고를 동시에 확인할 수 있다")
    void checkMultipleOptions() throws ExecutionException, InterruptedException {
        List<ProductOption> options = Arrays.asList(singleOption, bundleOption);
        Map<String, Integer> quantities = new HashMap<>();
        quantities.put("Single Option", 10);
        quantities.put("Bundle Option", 5);

        Map<String, Boolean> expectedResult = new HashMap<>();
        expectedResult.put("Single Option", true);
        expectedResult.put("Bundle Option", true);

        when(stockAvailabilityService.checkMultipleOptions(options, quantities))
                .thenReturn(CompletableFuture.completedFuture(expectedResult));

        CompletableFuture<Map<String, Boolean>> future = 
                stockAvailabilityService.checkMultipleOptions(options, quantities);
        Map<String, Boolean> result = future.get();

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedResult);
    }

    @Test
    @DisplayName("단일 SKU의 재고를 예약할 수 있다")
    void reserveStock() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        
        when(stockAvailabilityService.reserveStock("SKU001", 10, orderId))
                .thenReturn(CompletableFuture.completedFuture(true));

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveStock("SKU001", 10, orderId);
        Boolean result = future.get();

        assertThat(result).isTrue();
        verify(stockAvailabilityService).reserveStock("SKU001", 10, orderId);
    }

    @Test
    @DisplayName("재고가 부족하면 예약이 실패한다")
    void reserveStock_Failed() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        
        when(stockAvailabilityService.reserveStock("SKU001", 1000, orderId))
                .thenReturn(CompletableFuture.completedFuture(false));

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveStock("SKU001", 1000, orderId);
        Boolean result = future.get();

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("묶음 옵션의 재고를 예약할 수 있다")
    void reserveBundleStock() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        
        when(stockAvailabilityService.reserveBundleStock(bundleOption, 5, orderId))
                .thenReturn(CompletableFuture.completedFuture(true));

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveBundleStock(bundleOption, 5, orderId);
        Boolean result = future.get();

        assertThat(result).isTrue();
        verify(stockAvailabilityService).reserveBundleStock(bundleOption, 5, orderId);
    }

    @Test
    @DisplayName("묶음 옵션의 일부 SKU 재고가 부족하면 예약이 실패한다")
    void reserveBundleStock_PartiallyFailed() throws ExecutionException, InterruptedException {
        String orderId = "ORDER001";
        
        when(stockAvailabilityService.reserveBundleStock(bundleOption, 100, orderId))
                .thenReturn(CompletableFuture.completedFuture(false));

        CompletableFuture<Boolean> future = 
                stockAvailabilityService.reserveBundleStock(bundleOption, 100, orderId);
        Boolean result = future.get();

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예약을 해제할 수 있다")
    void releaseReservation() {
        String reservationId = "RESERVATION001";
        
        doNothing().when(stockAvailabilityService).releaseReservation(reservationId);

        stockAvailabilityService.releaseReservation(reservationId);

        verify(stockAvailabilityService).releaseReservation(reservationId);
    }

    @Test
    @DisplayName("단일 SKU의 가용 재고량을 조회할 수 있다")
    void getAvailableQuantity() {
        when(stockAvailabilityService.getAvailableQuantity("SKU001"))
                .thenReturn(50);

        int quantity = stockAvailabilityService.getAvailableQuantity("SKU001");

        assertThat(quantity).isEqualTo(50);
        verify(stockAvailabilityService).getAvailableQuantity("SKU001");
    }

    @Test
    @DisplayName("묶음 옵션의 가용 재고량을 조회할 수 있다")
    void getBundleAvailableQuantity() {
        Map<String, Integer> expectedQuantities = new HashMap<>();
        expectedQuantities.put("SKU001", 100);
        expectedQuantities.put("SKU002", 50);
        
        when(stockAvailabilityService.getBundleAvailableQuantity(bundleOption))
                .thenReturn(expectedQuantities);

        Map<String, Integer> quantities = 
                stockAvailabilityService.getBundleAvailableQuantity(bundleOption);

        assertThat(quantities).containsExactlyInAnyOrderEntriesOf(expectedQuantities);
        verify(stockAvailabilityService).getBundleAvailableQuantity(bundleOption);
    }

    @Test
    @DisplayName("존재하지 않는 SKU 조회 시 0을 반환한다")
    void getAvailableQuantity_NotFound() {
        when(stockAvailabilityService.getAvailableQuantity("INVALID_SKU"))
                .thenReturn(0);

        int quantity = stockAvailabilityService.getAvailableQuantity("INVALID_SKU");

        assertThat(quantity).isEqualTo(0);
    }
}