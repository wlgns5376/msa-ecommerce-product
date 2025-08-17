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
    
    @DisplayName("재고 조회 - 빈 문자열이나 공백으로만 이루어진 SKU ID로 조회 시 예외 발생")
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void execute_WithBlankSkuId_ShouldThrowIllegalArgumentException(String invalidSkuId) {
        // Given
        GetInventoryQuery query = createQuery(invalidSkuId);

        // When & Then
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID is required");

        verify(loadInventoryPort, never()).load(any());
    }
    
    @DisplayName("재고 조회 - 특수 문자가 포함된 SKU ID로 조회")
    @ParameterizedTest
    @ValueSource(strings = {"SKU@001", "SKU#123", "SKU$456", "SKU%789", "SKU&999", "SKU*000", "SKU!111", "SKU~222", "SKU`333"})
    void execute_WithSpecialCharacterSkuId_ShouldExecuteNormally(String skuIdWithSpecialChar) {
        // Given
        SkuId skuId = new SkuId(skuIdWithSpecialChar);
        Inventory inventory = createInventory(skuId, DEFAULT_TOTAL_QUANTITY, DEFAULT_RESERVED_QUANTITY);
        GetInventoryQuery query = createQuery(skuIdWithSpecialChar);
        
        mockInventoryExists(skuId, inventory);
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @DisplayName("재고 조회 - 매우 긴 SKU ID로 조회")
    @Test
    void execute_WithVeryLongSkuId_ShouldExecuteNormally() {
        // Given
        String longSkuId = "SKU-" + "A".repeat(100) + "-001";
        SkuId skuId = new SkuId(longSkuId);
        Inventory inventory = createInventory(skuId, DEFAULT_TOTAL_QUANTITY, DEFAULT_RESERVED_QUANTITY);
        GetInventoryQuery query = createQuery(longSkuId);
        
        mockInventoryExists(skuId, inventory);
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @DisplayName("재고 조회 - 한글이 포함된 SKU ID로 조회")
    @Test
    void execute_WithKoreanCharacterSkuId_ShouldExecuteNormally() {
        // Given
        String koreanSkuId = "SKU-제품-001";
        SkuId skuId = new SkuId(koreanSkuId);
        Inventory inventory = createInventory(skuId, DEFAULT_TOTAL_QUANTITY, DEFAULT_RESERVED_QUANTITY);
        GetInventoryQuery query = createQuery(koreanSkuId);
        
        mockInventoryExists(skuId, inventory);
        
        // When
        InventoryResponse response = getInventoryUseCase.execute(query);
        
        // Then
        assertThat(response)
            .isNotNull()
            .isEqualTo(InventoryResponse.from(inventory));
        
        verify(loadInventoryPort).load(skuId);
    }
    
    @DisplayName("재고 조회 - UUID 형식의 SKU ID로 조회")
    @Test
    void execute_WithUuidFormatSkuId_ShouldExecuteNormally() {
        // Given
        String uuidSkuId = UUID.randomUUID().toString();
        SkuId skuId = new SkuId(uuidSkuId);
        Inventory inventory = createInventory(skuId, DEFAULT_TOTAL_QUANTITY, DEFAULT_RESERVED_QUANTITY);
        GetInventoryQuery query = createQuery(uuidSkuId);
        
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
    @DisplayName("재고 조회 - null query로 조회 시 예외 발생")
    void execute_WithNullQuery_ShouldThrowIllegalArgumentException() {
        // Given
        GetInventoryQuery query = null;

        // When & Then
        assertThatThrownBy(() -> getInventoryUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class);

        verify(loadInventoryPort, never()).load(any());
    }
    
    @Test
    @DisplayName("정상적인 재고 조회 - 모든 재고가 예약된 경우")
    void execute_WithAllQuantityReserved_ShouldReturnZeroAvailable() {
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
}