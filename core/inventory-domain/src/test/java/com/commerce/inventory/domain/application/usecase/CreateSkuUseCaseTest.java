package com.commerce.inventory.domain.application.usecase;

import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.domain.repository.SkuRepository;
import com.commerce.inventory.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSkuUseCaseTest {
    
    @Mock
    private SkuRepository skuRepository;
    
    private Clock fixedClock;
    private CreateSkuUseCase useCase;
    
    @BeforeEach
    void setUp() {
        // 고정된 시간을 설정하여 테스트의 일관성 보장
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneId.of("UTC"));
        useCase = new CreateSkuUseCase(skuRepository, fixedClock);
    }
    
    @Test
    @DisplayName("정상적인 SKU 생성 요청시 SKU가 생성되어야 한다")
    void createSku_WithValidRequest_ShouldCreateSku() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-001")
                .name("테스트 상품")
                .description("테스트 상품 설명")
                .weight(1.5)
                .weightUnit("KILOGRAM")
                .volume(100.0)
                .volumeUnit("CUBIC_CM")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CreateSkuResponse response = useCase.execute(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getCode()).isEqualTo("SKU-001");
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getDescription()).isEqualTo("테스트 상품 설명");
        assertThat(response.getWeight()).isEqualTo(1.5);
        assertThat(response.getWeightUnit()).isEqualTo("KILOGRAM");
        assertThat(response.getVolume()).isEqualTo(100.0);
        assertThat(response.getVolumeUnit()).isEqualTo("CUBIC_CM");
        assertThat(response.getCreatedAt()).isNotNull();
        
        verify(skuRepository).findByCode(any(SkuCode.class));
        verify(skuRepository).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("중복된 SKU 코드로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithDuplicateCode_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-001")
                .name("테스트 상품")
                .build();
                
        Sku existingSku = Sku.create(
                CreateSkuCommand.builder()
                        .id(SkuId.generate())
                        .code(SkuCode.of("SKU-001"))
                        .name("기존 상품")
                        .build(),
                LocalDateTime.now()
        );
        
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.of(existingSku));
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(DuplicateSkuCodeException.class)
                .hasMessageContaining("이미 존재하는 SKU 코드입니다");
                
        verify(skuRepository).findByCode(any(SkuCode.class));
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("코드가 누락된 요청시 예외가 발생해야 한다")
    void createSku_WithMissingCode_ShouldThrowException() {
        // Given
        CreateSkuRequest requestWithoutCode = CreateSkuRequest.builder()
                .name("테스트 상품")
                .build();
                
        // When & Then
        assertThatThrownBy(() -> useCase.execute(requestWithoutCode))
                .isInstanceOf(InvalidSkuCodeException.class)
                .hasMessageContaining("SKU 코드는 필수입니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("이름이 누락된 요청시 예외가 발생해야 한다")
    void createSku_WithMissingName_ShouldThrowException() {
        // Given
        CreateSkuRequest requestWithoutName = CreateSkuRequest.builder()
                .code("SKU-001")
                .build();
                
        // When & Then
        assertThatThrownBy(() -> useCase.execute(requestWithoutName))
                .isInstanceOf(InvalidSkuException.class)
                .hasMessageContaining("SKU 이름은 필수입니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("무게와 부피 정보 없이도 SKU를 생성할 수 있어야 한다")
    void createSku_WithoutWeightAndVolume_ShouldCreateSku() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-002")
                .name("디지털 상품")
                .description("무게와 부피가 없는 디지털 상품")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CreateSkuResponse response = useCase.execute(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("SKU-002");
        assertThat(response.getName()).isEqualTo("디지털 상품");
        assertThat(response.getWeight()).isNull();
        assertThat(response.getWeightUnit()).isNull();
        assertThat(response.getVolume()).isNull();
        assertThat(response.getVolumeUnit()).isNull();
        
        verify(skuRepository).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("잘못된 형식의 SKU 코드로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithInvalidCodeFormat_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("invalid code!")  // 특수문자 포함
                .name("테스트 상품")
                .build();
                
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidSkuCodeException.class)
                .hasMessageContaining("SKU 코드는 영문자, 숫자, 하이픈, 언더스코어만 허용됩니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("음수 무게로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithNegativeWeight_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-003")
                .name("테스트 상품")
                .weight(-1.0)
                .weightUnit("KILOGRAM")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessageContaining("무게는 0보다 커야 합니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("음수 부피로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithNegativeVolume_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-004")
                .name("테스트 상품")
                .volume(-100.0)
                .volumeUnit("CUBIC_CM")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessageContaining("부피는 0보다 커야 합니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("잘못된 무게 단위로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithInvalidWeightUnit_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-005")
                .name("테스트 상품")
                .weight(100.0)
                .weightUnit("INVALID_UNIT")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessageContaining("유효하지 않은 무게 단위입니다: INVALID_UNIT");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("잘못된 부피 단위로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithInvalidVolumeUnit_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-006")
                .name("테스트 상품")
                .volume(100.0)
                .volumeUnit("INVALID_UNIT")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessageContaining("유효하지 않은 부피 단위입니다: INVALID_UNIT");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("대소문자가 다른 단위명도 처리할 수 있어야 한다")
    void createSku_WithDifferentCaseUnits_ShouldCreateSku() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-007")
                .name("테스트 상품")
                .weight(100.0)
                .weightUnit("kilogram")  // 소문자
                .volume(50.0)
                .volumeUnit("cubic_cm")  // 소문자
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CreateSkuResponse response = useCase.execute(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getWeight()).isEqualTo(100.0);
        assertThat(response.getWeightUnit()).isEqualTo("KILOGRAM");
        assertThat(response.getVolume()).isEqualTo(50.0);
        assertThat(response.getVolumeUnit()).isEqualTo("CUBIC_CM");
        
        verify(skuRepository).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("0 무게로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithZeroWeight_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-008")
                .name("테스트 상품")
                .weight(0.0)
                .weightUnit("KILOGRAM")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessageContaining("무게는 0보다 커야 합니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("0 부피로 생성 요청시 예외가 발생해야 한다")
    void createSku_WithZeroVolume_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-009")
                .name("테스트 상품")
                .volume(0.0)
                .volumeUnit("CUBIC_CM")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessageContaining("부피는 0보다 커야 합니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("무게 값만 제공하고 단위를 제공하지 않으면 예외가 발생해야 한다")
    void createSku_WithWeightButNoUnit_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-010")
                .name("테스트 상품")
                .weight(100.0)
                .weightUnit(null)
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessageContaining("무게와 무게 단위는 모두 제공되거나 모두 제공되지 않아야 합니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("무게 단위만 제공하고 값을 제공하지 않으면 예외가 발생해야 한다")
    void createSku_WithWeightUnitButNoValue_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-011")
                .name("테스트 상품")
                .weight(null)
                .weightUnit("KILOGRAM")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessageContaining("무게와 무게 단위는 모두 제공되거나 모두 제공되지 않아야 합니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("부피 값만 제공하고 단위를 제공하지 않으면 예외가 발생해야 한다")
    void createSku_WithVolumeButNoUnit_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-012")
                .name("테스트 상품")
                .volume(100.0)
                .volumeUnit(null)
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessageContaining("부피와 부피 단위는 모두 제공되거나 모두 제공되지 않아야 합니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("부피 단위만 제공하고 값을 제공하지 않으면 예외가 발생해야 한다")
    void createSku_WithVolumeUnitButNoValue_ShouldThrowException() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-013")
                .name("테스트 상품")
                .volume(null)
                .volumeUnit("CUBIC_CM")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidVolumeException.class)
                .hasMessageContaining("부피와 부피 단위는 모두 제공되거나 모두 제공되지 않아야 합니다");
                
        verify(skuRepository, never()).save(any(Sku.class));
    }
    
    @Test
    @DisplayName("생성된 SKU의 생성 시간이 고정된 Clock 시간과 일치해야 한다")
    void createSku_WithFixedClock_ShouldHaveExpectedCreatedAt() {
        // Given
        CreateSkuRequest request = CreateSkuRequest.builder()
                .code("SKU-TIME-TEST")
                .name("시간 테스트 상품")
                .build();
                
        when(skuRepository.findByCode(any(SkuCode.class))).thenReturn(Optional.empty());
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        LocalDateTime expectedTime = LocalDateTime.now(fixedClock);
        
        // When
        CreateSkuResponse response = useCase.execute(request);
        
        // Then
        assertThat(response.getCreatedAt()).isEqualTo(expectedTime);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
    }
}