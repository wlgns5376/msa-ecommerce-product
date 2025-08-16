package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.GetInventoryQuery;
import com.commerce.inventory.application.port.in.GetInventoryUseCase;
import com.commerce.inventory.application.port.in.InventoryResponse;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetInventoryUseCase 테스트")
class GetInventoryUseCaseTest {
    
    @Mock
    private LoadInventoryPort loadInventoryPort;
    
    @Mock
    private Validator validator;
    
    private GetInventoryUseCase getInventoryUseCase;
    
    @BeforeEach
    void setUp() {
        getInventoryUseCase = new GetInventoryService(loadInventoryPort, validator);
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
        
        when(validator.validate(query)).thenReturn(Set.of());
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
        verify(validator).validate(query);
    }
    
    @Test
    @DisplayName("재고 조회 - 재고가 존재하지 않는 경우 0으로 반환")
    void getInventory_WithNonExistingInventory_ReturnsZeroQuantities() {
        // Given
        String skuId = "SKU-999";
        GetInventoryQuery query = new GetInventoryQuery(skuId);
        
        when(validator.validate(query)).thenReturn(Set.of());
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
        verify(validator).validate(query);
    }
    
    @DisplayName("재고 조회 - null 또는 빈 문자열 SKU ID로 조회 시 예외 발생")
    @ParameterizedTest
    @NullAndEmptySource
    void getInventory_WithBlankSkuId_ThrowsException(String invalidSkuId) {
        // Given
        GetInventoryQuery query = new GetInventoryQuery(invalidSkuId);

        ConstraintViolation<GetInventoryQuery> violation = mock(ConstraintViolation.class);
        when(validator.validate(query)).thenReturn(Set.of(violation));

        // When & Then
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
                .isInstanceOf(ConstraintViolationException.class);

        verify(validator).validate(query);
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
        
        when(validator.validate(query)).thenReturn(Set.of());
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
        verify(validator).validate(query);
    }
    
    @Test
    @DisplayName("재고 조회 - null query로 조회 시 IllegalArgumentException 발생")
    void getInventory_WithNullQuery_ThrowsIllegalArgumentException() {
        // Given
        GetInventoryQuery query = null;
        
        // When & Then
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("GetInventoryQuery cannot be null.");
        
        verify(validator, never()).validate(any());
        verify(loadInventoryPort, never()).load(any());
    }
}