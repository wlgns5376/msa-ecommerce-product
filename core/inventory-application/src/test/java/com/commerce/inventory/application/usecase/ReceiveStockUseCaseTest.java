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
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneId.of("UTC"));
        useCase = new ReceiveStockService(
            loadSkuPort,
            loadInventoryPort,
            saveInventoryPort,
            saveStockMovementPort,
            fixedClock,
            validator
        );
    }
    
    @Test
    @DisplayName("정상적인 재고 입고 처리")
    void receiveStock_WithValidData_ShouldSucceed() {
        // Given
        SkuId skuId = SkuId.generate();
        SkuCode skuCode = SkuCode.of("SKU-001");
        Sku sku = Sku.create(skuId, skuCode, "테스트 상품", Weight.of(100, WeightUnit.GRAM), null, LocalDateTime.now(fixedClock));
        
        Inventory inventory = Inventory.createWithInitialStock(skuId, Quantity.of(100));
        
        ReceiveStockCommand command = ReceiveStockCommand.builder()
            .skuId(skuId.value())
            .quantity(50)
            .reference("PO-2024-001")
            .build();
        
        when(loadSkuPort.load(skuId)).thenReturn(Optional.of(sku));
        when(loadInventoryPort.load(skuId)).thenReturn(Optional.of(inventory));
        when(validator.validate(command)).thenReturn(Set.of());
        
        // When
        useCase.receive(command);
        
        // Then
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(saveInventoryPort).save(inventoryCaptor.capture());
        
        Inventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory.getTotalQuantity().value()).isEqualTo(150);
        assertThat(savedInventory.getAvailableQuantity().value()).isEqualTo(150);
        
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(saveStockMovementPort).save(movementCaptor.capture());
        
        StockMovement savedMovement = movementCaptor.getValue();
        assertThat(savedMovement.getSkuId()).isEqualTo(skuId);
        assertThat(savedMovement.getQuantity()).isEqualTo(Quantity.of(50));
        assertThat(savedMovement.getType()).isEqualTo(MovementType.RECEIVE);
        assertThat(savedMovement.getReference()).isEqualTo("PO-2024-001");
    }
    
    @Test
    @DisplayName("존재하지 않는 SKU에 대한 재고 입고 시 예외 발생")
    void receiveStock_WithNonExistentSku_ShouldThrowException() {
        // Given
        SkuId skuId = SkuId.generate();
        ReceiveStockCommand command = ReceiveStockCommand.builder()
            .skuId(skuId.value())
            .quantity(50)
            .reference("PO-2024-001")
            .build();
        
        when(loadSkuPort.load(skuId)).thenReturn(Optional.empty());
        when(validator.validate(command)).thenReturn(Set.of());
        
        // When & Then
        assertThatThrownBy(() -> useCase.receive(command))
            .isInstanceOf(InvalidSkuException.class)
            .hasMessageContaining("존재하지 않는 SKU입니다");
    }
    
    @Test
    @DisplayName("재고가 없는 SKU에 대한 첫 재고 입고")
    void receiveStock_WithNoExistingInventory_ShouldCreateNewInventory() {
        // Given
        SkuId skuId = SkuId.generate();
        SkuCode skuCode = SkuCode.of("SKU-002");
        Sku sku = Sku.create(skuId, skuCode, "신규 상품", Weight.of(200, WeightUnit.GRAM), null, LocalDateTime.now(fixedClock));
        
        ReceiveStockCommand command = ReceiveStockCommand.builder()
            .skuId(skuId.value())
            .quantity(100)
            .reference("PO-2024-002")
            .build();
        
        when(loadSkuPort.load(skuId)).thenReturn(Optional.of(sku));
        when(loadInventoryPort.load(skuId)).thenReturn(Optional.empty());
        when(validator.validate(command)).thenReturn(Set.of());
        
        // When
        useCase.receive(command);
        
        // Then
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(saveInventoryPort).save(inventoryCaptor.capture());
        
        Inventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory.getSkuId()).isEqualTo(skuId);
        assertThat(savedInventory.getTotalQuantity().value()).isEqualTo(100);
        assertThat(savedInventory.getReservedQuantity().value()).isEqualTo(0);
        assertThat(savedInventory.getAvailableQuantity().value()).isEqualTo(100);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -10})
    @DisplayName("0 또는 음수 수량으로 재고 입고 시 예외 발생")
    void receiveStock_WithInvalidQuantity_ShouldThrowException(int invalidQuantity) {
        // Given
        SkuId skuId = SkuId.generate();
        ReceiveStockCommand command = ReceiveStockCommand.builder()
            .skuId(skuId.value())
            .quantity(invalidQuantity)
            .reference("PO-2024-003")
            .build();
        
        // Mock validation failure
        ConstraintViolation<ReceiveStockCommand> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("입고 수량은 0보다 커야 합니다");
        when(validator.validate(command)).thenReturn(Set.of(violation));
        
        // When & Then
        assertThatThrownBy(() -> useCase.receive(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("입고 수량은 0보다 커야 합니다");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("참조 번호 없이 재고 입고 시 예외 발생")
    void receiveStock_WithoutReference_ShouldThrowException(String invalidReference) {
        // Given
        SkuId skuId = SkuId.generate();
        ReceiveStockCommand command = ReceiveStockCommand.builder()
            .skuId(skuId.value())
            .quantity(50)
            .reference(invalidReference)
            .build();
        
        // Mock validation failure
        ConstraintViolation<ReceiveStockCommand> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("참조 번호는 필수입니다");
        when(validator.validate(command)).thenReturn(Set.of(violation));
        
        // When & Then
        assertThatThrownBy(() -> useCase.receive(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("참조 번호는 필수입니다");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("SKU ID 없이 재고 입고 시 예외 발생")
    void receiveStock_WithoutSkuId_ShouldThrowException(String invalidSkuId) {
        // Given
        ReceiveStockCommand command = ReceiveStockCommand.builder()
            .skuId(invalidSkuId)
            .quantity(50)
            .reference("PO-2024-004")
            .build();
        
        // Mock validation failure
        ConstraintViolation<ReceiveStockCommand> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("SKU ID는 필수입니다");
        when(validator.validate(command)).thenReturn(Set.of(violation));
        
        // When & Then
        assertThatThrownBy(() -> useCase.receive(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SKU ID는 필수입니다");
    }
    
}