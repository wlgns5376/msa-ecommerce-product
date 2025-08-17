package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.GetInventoryQuery;
import com.commerce.inventory.application.port.in.GetInventoryUseCase;
import com.commerce.inventory.application.port.in.InventoryResponse;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    
    @BeforeEach
    void setUp() {
        // 단위 테스트에서는 Spring의 @Validated 기능이 적용되지 않음
        // 실제 Spring 환경에서는 @NotNull, @NotBlank 등의 어노테이션이 먼저 적용되어
        // ConstraintViolationException이 발생하게 됨
        // Spring 통합 테스트는 별도로 작성하여 실제 환경의 동작을 검증해야 함
        getInventoryUseCase = new GetInventoryService(loadInventoryPort);
    }
    
    // 테스트 픽스처 메서드
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
    
    private GetInventoryQuery createQuery(String skuId) {
        return new GetInventoryQuery(skuId);
    }
    
    @Test
    @DisplayName("정상적인 재고 조회 - 재고가 존재하는 경우")
    void execute_WithExistingInventory_ShouldReturnInventoryResponse() {
        // Given
        SkuId skuId = new SkuId(DEFAULT_SKU_ID);
        GetInventoryQuery query = createQuery(DEFAULT_SKU_ID);
        Inventory inventory = createDefaultInventory();
        
        when(loadInventoryPort.load(skuId))
            .thenReturn(Optional.of(inventory));
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @Test
    @DisplayName("재고 조회 - 재고가 존재하지 않는 경우 0으로 반환")
    void execute_WithNonExistingInventory_ShouldReturnZeroQuantities() {
        // Given
        SkuId skuId = new SkuId(NON_EXISTING_SKU_ID);
        GetInventoryQuery query = createQuery(NON_EXISTING_SKU_ID);
        
        when(loadInventoryPort.load(skuId))
            .thenReturn(Optional.empty());
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.empty(NON_EXISTING_SKU_ID));
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @DisplayName("재고 조회 - null, 빈 문자열, 공백으로만 이루어진 SKU ID로 조회 시 예외 발생")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void execute_WithBlankSkuId_ShouldThrowInvalidSkuIdException(String invalidSkuId) {
        // Given
        GetInventoryQuery query = createQuery(invalidSkuId);

        // When & Then
        // 단위 테스트 환경: SkuId 생성자에서 InvalidSkuIdException 발생
        // Spring 환경: GetInventoryQuery의 @NotBlank 검증으로 ConstraintViolationException 발생
        // 이 테스트는 도메인 레이어의 유효성 검사를 확인하며,
        // Spring 환경의 동작은 통합 테스트에서 별도로 검증해야 함
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
                .isInstanceOf(InvalidSkuIdException.class)
                .hasMessageContaining("SKU ID");

        verify(loadInventoryPort, never()).load(any());
    }
    
    @Test
    @DisplayName("재고 조회 - 모든 재고가 예약된 경우")
    void execute_WithAllQuantityReserved_ShouldReturnZeroAvailable() {
        // Given
        final int totalQuantity = 50;
        final int reservedQuantity = 50;
        SkuId skuId = new SkuId(DEFAULT_SKU_ID);
        GetInventoryQuery query = createQuery(DEFAULT_SKU_ID);
        Inventory inventory = createInventory(skuId, totalQuantity, reservedQuantity);
        
        when(loadInventoryPort.load(skuId))
            .thenReturn(Optional.of(inventory));
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        
        verify(loadInventoryPort).load(skuId);
    }
}