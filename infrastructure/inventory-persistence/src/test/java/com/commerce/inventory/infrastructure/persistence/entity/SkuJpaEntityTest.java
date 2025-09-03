package com.commerce.inventory.infrastructure.persistence.entity;

import com.commerce.inventory.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SkuJpaEntityTest {
    
    @Test
    @DisplayName("도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void fromDomainModel() {
        // Given
        SkuId skuId = SkuId.of(UUID.randomUUID().toString());
        SkuCode skuCode = SkuCode.of("SKU-001");
        String name = "맥북 프로 16인치 M3 Max";
        Weight weight = Weight.of(2160.0, WeightUnit.GRAM);
        Volume volume = Volume.of(5000.0, VolumeUnit.CUBIC_CM);
        
        Sku sku = Sku.create(
                skuId,
                skuCode,
                name,
                weight,
                volume,
                LocalDateTime.now()
        );
        
        // When
        SkuJpaEntity entity = SkuJpaEntity.fromDomainModel(sku);
        
        // Then
        assertThat(entity.getId()).isEqualTo(skuId.value());
        assertThat(entity.getCode()).isEqualTo(skuCode.value());
        assertThat(entity.getName()).isEqualTo(name);
        assertThat(entity.getDescription()).isNull(); // description은 create 메서드에서 설정되지 않음
        assertThat(entity.getWeightValue()).isEqualTo(weight.value());
        assertThat(entity.getWeightUnit()).isEqualTo(weight.unit());
        assertThat(entity.getVolumeValue()).isEqualTo(volume.value());
        assertThat(entity.getVolumeUnit()).isEqualTo(volume.unit());
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getVersion()).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("JPA 엔티티를 도메인 모델로 변환할 수 있다")
    void toDomainModel() {
        // Given
        String skuId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        SkuJpaEntity entity = SkuJpaEntity.builder()
                .id(skuId)
                .code("SKU-001")
                .name("맥북 프로 16인치 M3 Max")
                .description("최신 애플 실리콘 탑재")
                .weightValue(2160.0)
                .weightUnit(WeightUnit.GRAM)
                .volumeValue(5000.0)
                .volumeUnit(VolumeUnit.CUBIC_CM)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
        
        // When
        Sku sku = entity.toDomainModel();
        
        // Then
        assertThat(sku.getId().value()).isEqualTo(skuId);
        assertThat(sku.getCode().value()).isEqualTo("SKU-001");
        assertThat(sku.getName()).isEqualTo("맥북 프로 16인치 M3 Max");
        assertThat(sku.getDescription()).isEqualTo("최신 애플 실리콘 탑재");
        assertThat(sku.getWeight()).isNotNull();
        assertThat(sku.getWeight().value()).isEqualTo(2160.0);
        assertThat(sku.getWeight().unit()).isEqualTo(WeightUnit.GRAM);
        assertThat(sku.getVolume()).isNotNull();
        assertThat(sku.getVolume().value()).isEqualTo(5000.0);
        assertThat(sku.getVolume().unit()).isEqualTo(VolumeUnit.CUBIC_CM);
        assertThat(sku.getCreatedAt()).isEqualTo(now);
        assertThat(sku.getUpdatedAt()).isEqualTo(now);
        assertThat(sku.getVersion()).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("무게와 부피가 없는 SKU를 변환할 수 있다")
    void convertSkuWithoutWeightAndVolume() {
        // Given
        SkuId skuId = SkuId.of(UUID.randomUUID().toString());
        SkuCode skuCode = SkuCode.of("DIGITAL-001");
        String name = "소프트웨어 라이선스";
        String description = "디지털 상품";
        
        Sku sku = Sku.create(
                skuId,
                skuCode,
                name,
                null, // 무게 없음
                null, // 부피 없음
                LocalDateTime.now()
        );
        
        // When
        SkuJpaEntity entity = SkuJpaEntity.fromDomainModel(sku);
        Sku restoredSku = entity.toDomainModel();
        
        // Then
        assertThat(entity.getWeightValue()).isNull();
        assertThat(entity.getWeightUnit()).isNull();
        assertThat(entity.getVolumeValue()).isNull();
        assertThat(entity.getVolumeUnit()).isNull();
        assertThat(restoredSku.getWeight()).isNull();
        assertThat(restoredSku.getVolume()).isNull();
    }
    
    @Test
    @DisplayName("설명이 없는 SKU를 변환할 수 있다")
    void convertSkuWithoutDescription() {
        // Given
        SkuId skuId = SkuId.of(UUID.randomUUID().toString());
        SkuCode skuCode = SkuCode.of("SIMPLE-001");
        String name = "간단한 상품";
        
        Sku sku = Sku.create(
                skuId,
                skuCode,
                name,
                null,
                null,
                LocalDateTime.now()
        );
        
        // When
        SkuJpaEntity entity = SkuJpaEntity.fromDomainModel(sku);
        Sku restoredSku = entity.toDomainModel();
        
        // Then
        assertThat(entity.getDescription()).isNull();
        assertThat(restoredSku.getDescription()).isNull();
    }
    
    @Test
    @DisplayName("버전이 증가된 SKU를 변환할 수 있다")
    void convertSkuWithIncreasedVersion() {
        // Given
        SkuId skuId = SkuId.of(UUID.randomUUID().toString());
        SkuCode skuCode = SkuCode.of("VERSION-001");
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        
        // restore를 통해 버전이 있는 SKU 생성
        Sku sku = Sku.restore(
                skuId,
                skuCode,
                "버전 테스트 상품",
                "설명",
                Weight.of(1000.0, WeightUnit.GRAM),
                Volume.of(2000.0, VolumeUnit.CUBIC_CM),
                createdAt,
                updatedAt,
                5L // version
        );
        
        // When
        SkuJpaEntity entity = SkuJpaEntity.fromDomainModel(sku);
        Sku restoredSku = entity.toDomainModel();
        
        // Then
        assertThat(entity.getVersion()).isEqualTo(5L);
        assertThat(restoredSku.getVersion()).isEqualTo(5L);
    }
}