package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.GetInventoryQuery;
import com.commerce.inventory.application.port.in.GetInventoryUseCase;
import com.commerce.inventory.application.port.in.InventoryResponse;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetInventoryUseCase 테스트")
class GetInventoryUseCaseTest {
    
    @Mock
    private LoadInventoryPort loadInventoryPort;
    
    private GetInventoryUseCase getInventoryUseCase;
    
    @BeforeEach
    void setUp() {
        // 단위 테스트에서는 유효성 검사기를 직접 사용하지 않음
        // Spring 통합 테스트는 GetInventoryServiceSpringTest에서 수행
        getInventoryUseCase = new GetInventoryService(loadInventoryPort);
    }
    
    @Test
    @DisplayName("정상적인 재고 조회 - 재고가 존재하는 경우")
    void getInventory_WithExistingInventory_ReturnsInventoryResponse() {
        // Given
        String skuId = "SKU-001";
        GetInventoryQuery query = new GetInventoryQuery(skuId);
        
        Inventory inventory = Inventory.create(
            new SkuId(skuId),
            Quantity.of(100),
            Quantity.of(30)
        );
        
        when(loadInventoryPort.load(any(SkuId.class)))
            .thenReturn(Optional.of(inventory));
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.skuId()).isEqualTo(skuId);
        assertThat(response.totalQuantity()).isEqualTo(100);
        assertThat(response.reservedQuantity()).isEqualTo(30);
        assertThat(response.availableQuantity()).isEqualTo(70);
        
        verify(loadInventoryPort).load(any(SkuId.class));
    }
    
    @Test
    @DisplayName("재고 조회 - 재고가 존재하지 않는 경우 0으로 반환")
    void getInventory_WithNonExistingInventory_ReturnsZeroQuantities() {
        // Given
        String skuId = "SKU-999";
        GetInventoryQuery query = new GetInventoryQuery(skuId);
        
        when(loadInventoryPort.load(any(SkuId.class)))
            .thenReturn(Optional.empty());
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.skuId()).isEqualTo(skuId);
        assertThat(response.totalQuantity()).isEqualTo(0);
        assertThat(response.reservedQuantity()).isEqualTo(0);
        assertThat(response.availableQuantity()).isEqualTo(0);
        
        verify(loadInventoryPort).load(any(SkuId.class));
    }
    
    @DisplayName("재고 조회 - null 또는 빈 문자열 SKU ID로 조회 시 예외 발생")
    @ParameterizedTest
    @NullAndEmptySource
    void getInventory_WithBlankSkuId_ThrowsException(String invalidSkuId) {
        // Given
        GetInventoryQuery query = new GetInventoryQuery(invalidSkuId);

        // When & Then
        // 단위 테스트에서는 Spring 컨텍스트가 없어 SkuId 생성 시 InvalidSkuIdException이 발생
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
                .isInstanceOf(com.commerce.inventory.domain.exception.InvalidSkuIdException.class);

        verify(loadInventoryPort, never()).load(any());
    }
    
    @Test
    @DisplayName("재고 조회 - 모든 재고가 예약된 경우")
    void getInventory_WithAllQuantityReserved_ReturnsZeroAvailable() {
        // Given
        String skuId = "SKU-001";
        GetInventoryQuery query = new GetInventoryQuery(skuId);
        
        // 모든 재고가 예약된 상황
        Inventory inventory = Inventory.create(
            new SkuId(skuId),
            Quantity.of(50),
            Quantity.of(50)
        );
        
        when(loadInventoryPort.load(any(SkuId.class)))
            .thenReturn(Optional.of(inventory));
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.skuId()).isEqualTo(skuId);
        assertThat(response.totalQuantity()).isEqualTo(50);
        assertThat(response.reservedQuantity()).isEqualTo(50);
        assertThat(response.availableQuantity()).isEqualTo(0);
        
        verify(loadInventoryPort).load(any(SkuId.class));
    }
    
    @Test
    @DisplayName("재고 조회 - null query로 조회 시 NullPointerException 발생 (단위 테스트)")
    void getInventory_WithNullQuery_ThrowsNullPointerException() {
        // Given
        GetInventoryQuery query = null;
        
        // When & Then
        // 단위 테스트에서는 Spring 컨텍스트가 없어 선언적 유효성 검사가 작동하지 않음
        // Spring 통합 테스트는 GetInventoryServiceSpringTest에서 수행
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
            .isInstanceOf(NullPointerException.class);
        
        verify(loadInventoryPort, never()).load(any());
    }
}