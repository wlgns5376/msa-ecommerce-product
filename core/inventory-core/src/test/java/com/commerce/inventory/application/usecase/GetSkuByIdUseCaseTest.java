package com.commerce.inventory.application.usecase;

import com.commerce.inventory.application.service.GetSkuByIdService;
import com.commerce.inventory.domain.exception.SkuNotFoundException;
import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuCode;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.model.Volume;
import com.commerce.inventory.domain.model.VolumeUnit;
import com.commerce.inventory.domain.model.Weight;
import com.commerce.inventory.domain.model.WeightUnit;
import com.commerce.inventory.domain.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetSkuByIdUseCase 테스트")
class GetSkuByIdUseCaseTest {

    @Mock
    private SkuRepository skuRepository;

    @InjectMocks
    private GetSkuByIdService getSkuByIdService;

    private SkuId skuId;
    private Sku sku;
    private LocalDateTime currentTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now();
        skuId = SkuId.of("SKU-001");
        
        sku = Sku.restore(
            skuId,
            SkuCode.of("TEST-SKU-001"),
            "테스트 SKU",
            "테스트용 SKU 설명입니다",
            Weight.of(1.5, WeightUnit.KILOGRAM),
            Volume.of(10.0, VolumeUnit.CUBIC_M),
            currentTime.minusDays(1),
            currentTime.minusDays(1),
            1L
        );
    }

    @Test
    @DisplayName("SKU ID로 SKU를 성공적으로 조회한다")
    void should_get_sku_by_id_successfully() {
        // Given
        GetSkuByIdQuery query = GetSkuByIdQuery.of(skuId.value());
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));

        // When
        GetSkuByIdResponse response = getSkuByIdService.execute(query);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(skuId.value());
        assertThat(response.getCode()).isEqualTo("TEST-SKU-001");
        assertThat(response.getName()).isEqualTo("테스트 SKU");
        assertThat(response.getDescription()).isEqualTo("테스트용 SKU 설명입니다");
        assertThat(response.getWeight()).isEqualTo(BigDecimal.valueOf(1.5));
        assertThat(response.getVolume()).isEqualTo(BigDecimal.valueOf(10.0));
    }

    @Test
    @DisplayName("존재하지 않는 SKU ID로 조회 시 예외가 발생한다")
    void should_throw_exception_when_sku_not_found() {
        // Given
        String nonExistentId = "NON-EXISTENT-ID";
        GetSkuByIdQuery query = GetSkuByIdQuery.of(nonExistentId);
        when(skuRepository.findById(SkuId.of(nonExistentId))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> getSkuByIdService.execute(query))
            .isInstanceOf(SkuNotFoundException.class)
            .hasMessageContaining(nonExistentId);
    }

    @Test
    @DisplayName("null ID로 조회 시 예외가 발생한다")
    void should_throw_exception_when_id_is_null() {
        // Given & When & Then
        assertThatThrownBy(() -> GetSkuByIdQuery.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SKU ID는 필수입니다");
    }

    @Test
    @DisplayName("빈 ID로 조회 시 예외가 발생한다")
    void should_throw_exception_when_id_is_empty() {
        // Given & When & Then
        assertThatThrownBy(() -> GetSkuByIdQuery.of(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SKU ID는 필수입니다");
    }
}