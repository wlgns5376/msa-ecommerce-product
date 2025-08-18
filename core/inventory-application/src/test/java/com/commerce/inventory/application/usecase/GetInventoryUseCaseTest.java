package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.GetInventoryQuery;
import com.commerce.inventory.application.port.in.GetInventoryUseCase;
import com.commerce.inventory.application.port.in.InventoryResponse;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetInventoryUseCase 테스트")
class GetInventoryUseCaseTest {
    
    // 테스트 상수
    private static final String DEFAULT_SKU_ID = "SKU-001";
    private static final String NON_EXISTING_SKU_ID = "SKU-999";
    private static final int DEFAULT_TOTAL_QUANTITY = 100;
    private static final int DEFAULT_RESERVED_QUANTITY = 30;
    
    @Mock
    private LoadInventoryPort loadInventoryPort;
    
    private GetInventoryUseCase getInventoryUseCase;
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        getInventoryUseCase = new GetInventoryService(loadInventoryPort, validator);
    }
    
    private Inventory createInventory(SkuId skuId, int totalQuantity, int reservedQuantity) {
        return Inventory.create(
            skuId,
            Quantity.of(totalQuantity),
            Quantity.of(reservedQuantity)
        );
    }
    
    private Inventory createDefaultInventory() {
        return createInventory(new SkuId(DEFAULT_SKU_ID), DEFAULT_TOTAL_QUANTITY, DEFAULT_RESERVED_QUANTITY);
    }
    
    private GetInventoryQuery createDefaultQuery(SkuId skuId) {
        return new GetInventoryQuery(skuId.value());
    }
    
    private GetInventoryQuery createQuery(String skuId) {
        return new GetInventoryQuery(skuId);
    }
    
    private void mockInventoryExists(SkuId skuId, Inventory inventory) {
        when(loadInventoryPort.load(skuId)).thenReturn(Optional.of(inventory));
    }
    
    private void mockInventoryNotFound(SkuId skuId) {
        when(loadInventoryPort.load(skuId)).thenReturn(Optional.empty());
    }
    
    @Test
    @DisplayName("정상적인 재고 조회 - 재고가 존재하는 경우")
    void execute_WithExistingInventory_ShouldReturnInventoryResponse() {
        // Given
        SkuId skuId = new SkuId(DEFAULT_SKU_ID);
        Inventory inventory = createDefaultInventory();
        GetInventoryQuery query = createDefaultQuery(skuId);
        
        mockInventoryExists(skuId, inventory);
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @Test
    @DisplayName("정상적인 재고 조회 - 재고가 존재하지 않는 경우")
    void execute_WithNonExistingInventory_ShouldReturnZeroQuantities() {
        // Given
        SkuId skuId = new SkuId(NON_EXISTING_SKU_ID);
        GetInventoryQuery query = createDefaultQuery(skuId);
        
        mockInventoryNotFound(skuId);
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.empty(NON_EXISTING_SKU_ID));
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @DisplayName("재고 조회 - 빈 문자열이나 공백으로만 이루어진 SKU ID로 조회 시 유효성 검사 실패")
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void execute_WithBlankSkuId_ShouldThrowValidationException(String invalidSkuId) {
        // Given
        GetInventoryQuery query = createQuery(invalidSkuId);

        // When & Then
        assertValidationException(query, "SKU ID는 필수입니다");
    }
    
    @DisplayName("재고 조회 - 다양한 형식의 SKU ID로 조회")
    @ParameterizedTest
    @ValueSource(strings = {
        "SKU@001", "SKU#123", "SKU$456", "SKU%789", "SKU&999", "SKU*000", "SKU!111", "SKU~222", "SKU`333", // 특수 문자
        "SKU-제품-001", // 한글
        "123e4567-e89b-12d3-a456-426614174000" // UUID 형식
    })
    void execute_WithVariousSkuIdFormats_ShouldExecuteNormally(String skuId) {
        assertSuccessfulInventoryQuery(skuId, DEFAULT_TOTAL_QUANTITY, DEFAULT_RESERVED_QUANTITY);
    }
    
    @DisplayName("재고 조회 - 매우 긴 SKU ID로 조회")
    @Test
    void execute_WithVeryLongSkuId_ShouldExecuteNormally() {
        // Given
        String longSkuId = "SKU-" + "A".repeat(100) + "-001";
        
        // When & Then
        assertSuccessfulInventoryQuery(longSkuId, DEFAULT_TOTAL_QUANTITY, DEFAULT_RESERVED_QUANTITY);
    }
    
    @Test
    @DisplayName("재고 조회 - null query로 조회 시 유효성 검사 실패")
    void execute_WithNullQuery_ShouldThrowValidationException() {
        // Given
        GetInventoryQuery query = null;

        // When & Then
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class);

        verify(loadInventoryPort, never()).load(any());
    }
    
    @Test
    @DisplayName("재고 조회 - null SKU ID를 가진 쿼리 실행 시 유효성 검사 실패")
    void createQuery_WithNullSkuId_ShouldBeHandledByValidation() {
        // Given
        String nullSkuId = null;
        
        // When
        GetInventoryQuery query = new GetInventoryQuery(nullSkuId);
        
        // Then - 쿼리 객체는 생성되지만 실행 시 유효성 검사에서 실패
        assertThat(query.skuId()).isNull();
        
        // 유효성 검사 실행 시 실패 확인
        assertValidationException(query, "SKU ID는 필수입니다");
    }
    
    @Test
    @DisplayName("정상적인 재고 조회 - 모든 재고가 예약된 경우 (가용 재고 0)")
    void execute_WithAllQuantityReserved_ShouldReturnZeroAvailableQuantity() {
        // Given
        final int totalQuantity = 50;
        final int reservedQuantity = 50;
        SkuId skuId = new SkuId(DEFAULT_SKU_ID);
        Inventory inventory = createInventory(skuId, totalQuantity, reservedQuantity);
        GetInventoryQuery query = createDefaultQuery(skuId);
        
        mockInventoryExists(skuId, inventory);
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @Test
    @DisplayName("재고 조회 - 동시 다발적인 조회 요청 처리")
    void execute_WithConcurrentRequests_ShouldHandleCorrectly() throws InterruptedException {
        // Given
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        
        SkuId skuId = new SkuId(DEFAULT_SKU_ID);
        Inventory inventory = createDefaultInventory();
        GetInventoryQuery query = createDefaultQuery(skuId);
        
        mockInventoryExists(skuId, inventory);
        
        try {
            // When
            for (int i = 0; i < threadCount; i++) {
                executorService.execute(() -> {
                    try {
                        InventoryResponse response = getInventoryUseCase.execute(query);
                        if (response != null && response.equals(InventoryResponse.from(inventory))) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue().withFailMessage("동시성 테스트가 타임아웃 내에 완료되지 않았습니다");
            
            // Then
            assertThat(successCount.get()).isEqualTo(threadCount);
            assertThat(failureCount.get()).isZero();
            verify(loadInventoryPort, times(threadCount)).load(skuId);
        } finally {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }
    
    @Test
    @DisplayName("재고 조회 - 재고가 0인 경우 정상 조회")
    void execute_WithZeroInventory_ShouldReturnZeroQuantities() {
        // Given
        SkuId skuId = new SkuId(DEFAULT_SKU_ID);
        Inventory inventory = createInventory(skuId, 0, 0);
        GetInventoryQuery query = createDefaultQuery(skuId);
        
        mockInventoryExists(skuId, inventory);
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        assertThat(response.totalQuantity()).isZero();
        assertThat(response.reservedQuantity()).isZero();
        assertThat(response.availableQuantity()).isZero();
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @Test
    @DisplayName("GetInventoryQuery 유효성 검사 - 직접 검증")
    void validateGetInventoryQuery_WithInvalidData_ShouldHaveViolations() {
        // Given
        GetInventoryQuery queryWithBlankSkuId = new GetInventoryQuery("   ");
        GetInventoryQuery queryWithNullSkuId = new GetInventoryQuery(null);
        
        // When
        Set<ConstraintViolation<GetInventoryQuery>> blankViolations = validator.validate(queryWithBlankSkuId);
        Set<ConstraintViolation<GetInventoryQuery>> nullViolations = validator.validate(queryWithNullSkuId);
        
        // Then
        assertThat(blankViolations).isNotEmpty();
        assertThat(blankViolations.iterator().next().getMessage()).isEqualTo("SKU ID는 필수입니다");
        
        assertThat(nullViolations).isNotEmpty();
        assertThat(nullViolations.iterator().next().getMessage()).isEqualTo("SKU ID는 필수입니다");
    }
    
    private void assertValidationException(GetInventoryQuery query, String expectedMessage) {
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
                
        verify(loadInventoryPort, never()).load(any());
    }
    
    private void assertSuccessfulInventoryQuery(String skuIdValue, int totalQuantity, int reservedQuantity) {
        // Given
        SkuId skuId = new SkuId(skuIdValue);
        Inventory inventory = createInventory(skuId, totalQuantity, reservedQuantity);
        GetInventoryQuery query = createQuery(skuIdValue);
        
        mockInventoryExists(skuId, inventory);
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        
        verify(loadInventoryPort).load(skuId);
    }
}