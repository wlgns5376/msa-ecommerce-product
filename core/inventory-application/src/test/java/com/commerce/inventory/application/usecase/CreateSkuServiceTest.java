package com.commerce.inventory.application.usecase;

import com.commerce.inventory.application.port.in.CreateSkuCommand;
import com.commerce.inventory.application.port.in.CreateSkuResponse;
import com.commerce.inventory.application.port.out.LoadSkuPort;
import com.commerce.inventory.application.port.out.SaveSkuPort;
import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSkuServiceTest {
    
    @Mock
    private LoadSkuPort loadSkuPort;
    
    @Mock
    private SaveSkuPort saveSkuPort;
    
    private Clock fixedClock;
    private CreateSkuService createSkuService;
    
    @BeforeEach
    void setUp() {
        // 고정된 시간을 설정하여 테스트의 일관성 보장
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneId.of("UTC"));
        createSkuService = new CreateSkuService(loadSkuPort, saveSkuPort, fixedClock);
    }
    
    @Test
    @DisplayName("정상적인 SKU 생성 요청시 SKU가 생성되어야 한다")
    void createSku_WithValidRequest_ShouldCreateSku() {
        // Given
        CreateSkuCommand command = CreateSkuCommand.builder()
                .code("SKU-001")
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .weight(1.5)
                .weightUnit("KILOGRAM")
                .volume(10.0)
                .volumeUnit("LITER")
                .build();
        
        when(loadSkuPort.existsByCode(any(SkuCode.class))).thenReturn(false);
        
        Sku savedSku = Sku.create(
                com.commerce.inventory.domain.model.CreateSkuCommand.builder()
                        .id(SkuId.generate())
                        .code(SkuCode.of("SKU-001"))
                        .name("테스트 상품")
                        .description("테스트 상품 설명")
                        .weight(Weight.of(1.5, WeightUnit.KILOGRAM))
                        .volume(Volume.of(10.0, VolumeUnit.LITER))
                        .build(),
                LocalDateTime.now(fixedClock)
        );
        
        when(saveSkuPort.save(any(Sku.class))).thenReturn(savedSku);
        
        // When
        CreateSkuResponse response = createSkuService.execute(command);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("SKU-001");
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getDescription()).isEqualTo("테스트 상품 설명");
        assertThat(response.getWeight()).isEqualTo(1.5);
        assertThat(response.getWeightUnit()).isEqualTo("KILOGRAM");
        assertThat(response.getVolume()).isEqualTo(10.0);
        assertThat(response.getVolumeUnit()).isEqualTo("LITER");
        
        verify(loadSkuPort).existsByCode(any(SkuCode.class));
        verify(saveSkuPort).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("이미 존재하는 SKU 코드로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithExistingCode_ShouldThrowException() {
        // Given
        CreateSkuCommand command = CreateSkuCommand.builder()
                .code("SKU-001")
                .name("테스트 상품")
                .build();
        
        when(loadSkuPort.existsByCode(any(SkuCode.class))).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> createSkuService.execute(command))
                .isInstanceOf(DuplicateSkuCodeException.class)
                .hasMessageContaining("이미 존재하는 SKU 코드입니다");
        
        verify(loadSkuPort).existsByCode(any(SkuCode.class));
        verify(saveSkuPort, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("무게 값은 있지만 단위가 없으면 예외가 발생해야 한다")
    void createSku_WithWeightButNoUnit_ShouldThrowException() {
        // Given
        CreateSkuCommand command = CreateSkuCommand.builder()
                .code("SKU-001")
                .name("테스트 상품")
                .weight(1.5)
                .weightUnit(null)
                .build();
        
        when(loadSkuPort.existsByCode(any(SkuCode.class))).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> createSkuService.execute(command))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessageContaining("무게와 무게 단위는 모두 제공되거나 모두 제공되지 않아야 합니다");
    }
    
    @Test
    @DisplayName("데이터베이스 제약 조건 위반시 DuplicateSkuCodeException이 발생해야 한다")
    void createSku_WithDataIntegrityViolation_ShouldThrowDuplicateException() {
        // Given
        CreateSkuCommand command = CreateSkuCommand.builder()
                .code("SKU-001")
                .name("테스트 상품")
                .build();
        
        when(loadSkuPort.existsByCode(any(SkuCode.class))).thenReturn(false);
        when(saveSkuPort.save(any(Sku.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));
        
        // When & Then
        assertThatThrownBy(() -> createSkuService.execute(command))
                .isInstanceOf(DuplicateSkuCodeException.class)
                .hasMessageContaining("SKU 코드 'SKU-001'가 이미 존재하여 생성에 실패했습니다");
    }
}