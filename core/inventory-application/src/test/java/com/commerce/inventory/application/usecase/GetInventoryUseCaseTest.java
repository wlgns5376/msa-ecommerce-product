package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.GetInventoryQuery;
import com.commerce.inventory.application.port.in.GetInventoryUseCase;
import com.commerce.inventory.application.port.in.InventoryResponse;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        assertThat(response.getSkuId()).isEqualTo(skuId);
        assertThat(response.getTotalQuantity()).isEqualTo(100);
        assertThat(response.getReservedQuantity()).isEqualTo(30);
        assertThat(response.getAvailableQuantity()).isEqualTo(70);
        
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
        assertThat(response.getSkuId()).isEqualTo(skuId);
        assertThat(response.getTotalQuantity()).isEqualTo(0);
        assertThat(response.getReservedQuantity()).isEqualTo(0);
        assertThat(response.getAvailableQuantity()).isEqualTo(0);
        
        verify(loadInventoryPort).load(any(SkuId.class));
        verify(validator).validate(query);
    }
    
    @Test
    @DisplayName("재고 조회 - null SKU ID로 조회 시 예외 발생")
    void getInventory_WithNullSkuId_ThrowsException() {
        // Given
        GetInventoryQuery query = new GetInventoryQuery(null);
        
        ConstraintViolation<GetInventoryQuery> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("SKU ID is required");
        when(validator.validate(query)).thenReturn(Set.of(violation));
        
        // When & Then
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
            .isInstanceOf(ConstraintViolationException.class);
        
        verify(validator).validate(query);
        verify(loadInventoryPort, never()).load(any());
    }
    
    @Test
    @DisplayName("재고 조회 - 잘못된 형식의 SKU ID로 조회 시 예외 발생")
    void getInventory_WithInvalidSkuIdFormat_ThrowsException() {
        // Given
        String invalidSkuId = "";
        GetInventoryQuery query = new GetInventoryQuery(invalidSkuId);
        
        when(validator.validate(query)).thenReturn(Set.of());
        
        // When & Then
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
            .isInstanceOf(InvalidSkuIdException.class)
            .hasMessageContaining("SKU ID는 필수입니다");
        
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
        assertThat(response.getSkuId()).isEqualTo(skuId);
        assertThat(response.getTotalQuantity()).isEqualTo(50);
        assertThat(response.getReservedQuantity()).isEqualTo(50);
        assertThat(response.getAvailableQuantity()).isEqualTo(0);
        
        verify(loadInventoryPort).load(any(SkuId.class));
        verify(validator).validate(query);
    }
}