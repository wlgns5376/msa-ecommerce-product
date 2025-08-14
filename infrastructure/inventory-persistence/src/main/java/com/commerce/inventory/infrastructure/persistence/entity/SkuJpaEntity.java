package com.commerce.inventory.infrastructure.persistence.entity;

import com.commerce.inventory.domain.model.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "skus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SkuJpaEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;
    
    @Column(name = "code", nullable = false, unique = true)
    private String code;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "weight_value")
    private Double weightValue;
    
    @Column(name = "weight_unit")
    @Enumerated(EnumType.STRING)
    private WeightUnit weightUnit;
    
    @Column(name = "volume_value")
    private Double volumeValue;
    
    @Column(name = "volume_unit")
    @Enumerated(EnumType.STRING)
    private VolumeUnit volumeUnit;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    public static SkuJpaEntity fromDomainModel(Sku sku) {
        return SkuJpaEntity.builder()
                .id(sku.getId().value())
                .code(sku.getCode().value())
                .name(sku.getName())
                .description(sku.getDescription())
                .weightValue(sku.getWeight() != null ? sku.getWeight().value() : null)
                .weightUnit(sku.getWeight() != null ? sku.getWeight().unit() : null)
                .volumeValue(sku.getVolume() != null ? sku.getVolume().value() : null)
                .volumeUnit(sku.getVolume() != null ? sku.getVolume().unit() : null)
                .createdAt(sku.getCreatedAt())
                .updatedAt(sku.getUpdatedAt())
                .version(sku.getVersion())
                .build();
    }
    
    public Sku toDomainModel() {
        Weight weight = null;
        if (weightValue != null && weightUnit != null) {
            weight = Weight.of(weightValue, weightUnit);
        }
        
        Volume volume = null;
        if (volumeValue != null && volumeUnit != null) {
            volume = Volume.of(volumeValue, volumeUnit);
        }
        
        return Sku.restore(
                SkuId.of(id),
                SkuCode.of(code),
                name,
                description,
                weight,
                volume,
                createdAt,
                updatedAt,
                version
        );
    }
}