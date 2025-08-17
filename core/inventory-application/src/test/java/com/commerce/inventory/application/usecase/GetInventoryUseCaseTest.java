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
    
    private static final String SKU_ID_VALUE = "SKU-001";
    private static final SkuId SKU_ID = new SkuId(SKU_ID_VALUE);
    private static final GetInventoryQuery QUERY = new GetInventoryQuery(SKU_ID_VALUE);
    
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
    
    private Inventory createInventory(int totalQuantity, int reservedQuantity) {
        return Inventory.create(
            SKU_ID,
            Quantity.of(totalQuantity),
            Quantity.of(reservedQuantity)
        );
    }
    
    @Test
    @DisplayName("정상적인 재고 조회 - 재고가 존재하는 경우")
    void getInventory_WithExistingInventory_ReturnsInventoryResponse() {
        // Given
        Inventory inventory = createInventory(100, 30);
        
        when(loadInventoryPort.load(SKU_ID))
            .thenReturn(Optional.of(inventory));
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(QUERY);
        
        // Then
        InventoryResponse expectedResponse = InventoryResponse.from(inventory);
        assertThat(response).isEqualTo(expectedResponse);
        
        verify(loadInventoryPort).load(SKU_ID);
    }
    
    @Test
    @DisplayName("재고 조회 - 재고가 존재하지 않는 경우 0으로 반환")
    void getInventory_WithNonExistingInventory_ReturnsZeroQuantities() {
        // Given
        String skuIdValue = "SKU-999";
        SkuId skuId = new SkuId(skuIdValue);
        GetInventoryQuery query = new GetInventoryQuery(skuIdValue);
        
        when(loadInventoryPort.load(skuId))
            .thenReturn(Optional.empty());
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        InventoryResponse expectedResponse = InventoryResponse.empty(skuIdValue);
        assertThat(response).isEqualTo(expectedResponse);
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @DisplayName("재고 조회 - null, 빈 문자열, 공백으로만 이루어진 SKU ID로 조회 시 예외 발생")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void getInventory_WithBlankSkuId_ThrowsException(String invalidSkuId) {
        // Given
        GetInventoryQuery query = new GetInventoryQuery(invalidSkuId);

        // When & Then
        // 단위 테스트 환경: SkuId 생성자에서 InvalidSkuIdException 발생
        // Spring 환경: GetInventoryQuery의 @NotBlank 검증으로 ConstraintViolationException 발생
        // 이 테스트는 도메인 레이어의 유효성 검사를 확인하며,
        // Spring 환경의 동작은 통합 테스트에서 별도로 검증해야 함
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
                .isInstanceOf(InvalidSkuIdException.class);

        verify(loadInventoryPort, never()).load(any());
    }
    
    @Test
    @DisplayName("재고 조회 - 모든 재고가 예약된 경우")
    void getInventory_WithAllQuantityReserved_ReturnsZeroAvailable() {
        // Given
        // 모든 재고가 예약된 상황
        Inventory inventory = createInventory(50, 50);
        
        when(loadInventoryPort.load(SKU_ID))
            .thenReturn(Optional.of(inventory));
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(QUERY);
        
        // Then
        InventoryResponse expectedResponse = InventoryResponse.from(inventory);
        assertThat(response).isEqualTo(expectedResponse);
        
        verify(loadInventoryPort).load(SKU_ID);
    }
}