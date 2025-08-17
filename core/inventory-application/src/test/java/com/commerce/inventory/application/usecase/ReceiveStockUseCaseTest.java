package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.ReceiveStockCommand;
import com.commerce.inventory.application.port.in.ReceiveStockUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.LoadSkuPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.application.port.out.SaveStockMovementPort;
import com.commerce.inventory.domain.exception.InvalidSkuException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.MovementType;
import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuCode;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.model.StockMovement;
import com.commerce.inventory.domain.model.Weight;
import com.commerce.inventory.domain.model.WeightUnit;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiveStockUseCase 테스트")
class ReceiveStockUseCaseTest {

    // 테스트 상수
    private static final String DEFAULT_SKU_CODE = "SKU-001";
    private static final String DEFAULT_REFERENCE = "PO-2024-001";
    private static final int DEFAULT_QUANTITY = 50;
    private static final int DEFAULT_INITIAL_STOCK = 100;
    private static final String FIXED_TIME_STRING = "2024-01-01T10:00:00Z";
    
    @Mock
    private LoadSkuPort loadSkuPort;
    
    @Mock
    private LoadInventoryPort loadInventoryPort;
    
    @Mock
    private SaveInventoryPort saveInventoryPort;
    
    @Mock
    private SaveStockMovementPort saveStockMovementPort;
    
    @Mock
    private Validator validator;
    
    private Clock fixedClock;
    private ReceiveStockUseCase useCase;
    
    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse(FIXED_TIME_STRING), ZoneId.of("UTC"));
        useCase = new ReceiveStockService(
            loadSkuPort,
            loadInventoryPort,
            saveInventoryPort,
            saveStockMovementPort,
            fixedClock,
            validator
        );
    }
    
    // 테스트 픽스처 메서드
    private ReceiveStockCommand createCommand(String skuId, int quantity, String reference) {
        return ReceiveStockCommand.builder()
            .skuId(skuId)
            .quantity(quantity)
            .reference(reference)
            .build();
    }
    
    private ReceiveStockCommand createDefaultCommand(SkuId skuId) {
        return createCommand(skuId.value(), DEFAULT_QUANTITY, DEFAULT_REFERENCE);
    }
    
    private Sku createDefaultSku(SkuId skuId) {
        return Sku.create(
            skuId,
            SkuCode.of(DEFAULT_SKU_CODE),
            "테스트 상품",
            Weight.of(100, WeightUnit.GRAM),
            null,
            LocalDateTime.now(fixedClock)
        );
    }
    
    private void mockValidationSuccess(ReceiveStockCommand command) {
        when(validator.validate(command)).thenReturn(Set.of());
    }
    
    private void mockValidationFailure(ReceiveStockCommand command, String errorMessage) {
        ConstraintViolation<ReceiveStockCommand> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn(errorMessage);
        when(validator.validate(command)).thenReturn(Set.of(violation));
    }
    
    @Test
    @DisplayName("정상적인 재고 입고 처리")
    void receive_WithValidData_ShouldSucceed() {
        // Given
        SkuId skuId = SkuId.generate();
        Inventory inventory = Inventory.createWithInitialStock(skuId, Quantity.of(DEFAULT_INITIAL_STOCK));
        ReceiveStockCommand command = createDefaultCommand(skuId);
        
        when(loadSkuPort.exists(skuId)).thenReturn(true);
        when(loadInventoryPort.load(skuId)).thenReturn(Optional.of(inventory));
        mockValidationSuccess(command);
        
        // When
        useCase.receive(command);
        
        // Then
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(saveInventoryPort).save(inventoryCaptor.capture());
        
        Inventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory.getTotalQuantity().value())
            .isEqualTo(DEFAULT_INITIAL_STOCK + DEFAULT_QUANTITY);
        assertThat(savedInventory.getAvailableQuantity().value())
            .isEqualTo(DEFAULT_INITIAL_STOCK + DEFAULT_QUANTITY);
        
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(saveStockMovementPort).save(movementCaptor.capture());
        
        StockMovement savedMovement = movementCaptor.getValue();
        assertThat(savedMovement.getSkuId()).isEqualTo(skuId);
        assertThat(savedMovement.getQuantity()).isEqualTo(Quantity.of(DEFAULT_QUANTITY));
        assertThat(savedMovement.getType()).isEqualTo(MovementType.RECEIVE);
        assertThat(savedMovement.getReference()).isEqualTo(DEFAULT_REFERENCE);
    }
    
    @Test
    @DisplayName("존재하지 않는 SKU에 대한 재고 입고 시 예외 발생")
    void receive_WithNonExistentSku_ShouldThrowInvalidSkuException() {
        // Given
        SkuId skuId = SkuId.generate();
        ReceiveStockCommand command = createDefaultCommand(skuId);
        
        when(loadSkuPort.exists(skuId)).thenReturn(false);
        mockValidationSuccess(command);
        
        // When & Then
        assertThatThrownBy(() -> useCase.receive(command))
            .isInstanceOf(InvalidSkuException.class)
            .hasMessageContaining("존재하지 않는 SKU입니다");
    }
    
    @Test
    @DisplayName("재고가 없는 SKU에 대한 첫 재고 입고")
    void receive_WithNoExistingInventory_ShouldCreateNewInventory() {
        // Given
        SkuId skuId = SkuId.generate();
        final int initialQuantity = 100;
        ReceiveStockCommand command = createCommand(skuId.value(), initialQuantity, "PO-2024-002");
        
        when(loadSkuPort.exists(skuId)).thenReturn(true);
        when(loadInventoryPort.load(skuId)).thenReturn(Optional.empty());
        mockValidationSuccess(command);
        
        // When
        useCase.receive(command);
        
        // Then
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(saveInventoryPort).save(inventoryCaptor.capture());
        
        Inventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory.getSkuId()).isEqualTo(skuId);
        assertThat(savedInventory.getTotalQuantity().value()).isEqualTo(initialQuantity);
        assertThat(savedInventory.getReservedQuantity().value()).isZero();
        assertThat(savedInventory.getAvailableQuantity().value()).isEqualTo(initialQuantity);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -10})
    @DisplayName("0 또는 음수 수량으로 재고 입고 시 예외 발생")
    void receive_WithInvalidQuantity_ShouldThrowIllegalArgumentException(int invalidQuantity) {
        // Given
        SkuId skuId = SkuId.generate();
        ReceiveStockCommand command = createCommand(skuId.value(), invalidQuantity, "PO-2024-003");
        
        final String errorMessage = "입고 수량은 0보다 커야 합니다";
        mockValidationFailure(command, errorMessage);
        
        // When & Then
        assertThatThrownBy(() -> useCase.receive(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(errorMessage);
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("참조 번호 없이 재고 입고 시 예외 발생")
    void receive_WithoutReference_ShouldThrowIllegalArgumentException(String invalidReference) {
        // Given
        SkuId skuId = SkuId.generate();
        ReceiveStockCommand command = createCommand(skuId.value(), DEFAULT_QUANTITY, invalidReference);
        
        final String errorMessage = "참조 번호는 필수입니다";
        mockValidationFailure(command, errorMessage);
        
        // When & Then
        assertThatThrownBy(() -> useCase.receive(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(errorMessage);
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("SKU ID 없이 재고 입고 시 예외 발생")
    void receive_WithoutSkuId_ShouldThrowIllegalArgumentException(String invalidSkuId) {
        // Given
        ReceiveStockCommand command = createCommand(invalidSkuId, DEFAULT_QUANTITY, "PO-2024-004");
        
        final String errorMessage = "SKU ID는 필수입니다";
        mockValidationFailure(command, errorMessage);
        
        // When & Then
        assertThatThrownBy(() -> useCase.receive(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(errorMessage);
    }
    
}