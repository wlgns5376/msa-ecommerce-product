package com.commerce.inventory.domain.application.usecase;

import com.commerce.inventory.domain.exception.DuplicateSkuCodeException;
import com.commerce.inventory.domain.exception.InvalidSkuCodeException;
import com.commerce.inventory.domain.exception.InvalidVolumeException;
import com.commerce.inventory.domain.exception.InvalidWeightException;
import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.domain.repository.SkuRepository;
import com.commerce.product.domain.application.usecase.UseCase;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class CreateSkuUseCase implements UseCase<CreateSkuRequest, CreateSkuResponse> {
    
    private final SkuRepository skuRepository;
    
    @Override
    public CreateSkuResponse execute(CreateSkuRequest request) {
        validateRequest(request);
        
        SkuCode skuCode = createSkuCode(request.getCode());
        
        checkDuplicateCode(skuCode);
        
        CreateSkuCommand command = buildCreateCommand(request, skuCode);
        
        Sku sku = Sku.create(command, LocalDateTime.now());
        
        Sku savedSku = skuRepository.save(sku);
        
        return mapToResponse(savedSku);
    }
    
    private void validateRequest(CreateSkuRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("SKU 코드는 필수입니다");
        }
        
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("SKU 이름은 필수입니다");
        }
    }
    
    private SkuCode createSkuCode(String code) {
        return SkuCode.of(code);
    }
    
    private void checkDuplicateCode(SkuCode skuCode) {
        skuRepository.findByCode(skuCode)
                .ifPresent(sku -> {
                    throw new DuplicateSkuCodeException("이미 존재하는 SKU 코드입니다: " + skuCode.getValue());
                });
    }
    
    private CreateSkuCommand buildCreateCommand(CreateSkuRequest request, SkuCode skuCode) {
        CreateSkuCommand.CreateSkuCommandBuilder builder = CreateSkuCommand.builder()
                .id(SkuId.generate())
                .code(skuCode)
                .name(request.getName())
                .description(request.getDescription());
        
        if (request.getWeight() != null && request.getWeightUnit() != null) {
            validateWeight(request.getWeight());
            WeightUnit weightUnit;
            try {
                weightUnit = WeightUnit.valueOf(request.getWeightUnit().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidWeightException("유효하지 않은 무게 단위입니다: " + request.getWeightUnit());
            }
            builder.weight(Weight.of(request.getWeight(), weightUnit));
        }
        
        if (request.getVolume() != null && request.getVolumeUnit() != null) {
            validateVolume(request.getVolume());
            VolumeUnit volumeUnit;
            try {
                volumeUnit = VolumeUnit.valueOf(request.getVolumeUnit().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidVolumeException("유효하지 않은 부피 단위입니다: " + request.getVolumeUnit());
            }
            builder.volume(Volume.of(request.getVolume(), volumeUnit));
        }
        
        return builder.build();
    }
    
    private void validateWeight(Double weight) {
        if (weight <= 0) {
            throw new InvalidWeightException("무게는 0보다 커야 합니다");
        }
    }
    
    private void validateVolume(Double volume) {
        if (volume <= 0) {
            throw new InvalidVolumeException("부피는 0보다 커야 합니다");
        }
    }
    
    private CreateSkuResponse mapToResponse(Sku sku) {
        CreateSkuResponse.CreateSkuResponseBuilder builder = CreateSkuResponse.builder()
                .id(sku.getId().getValue())
                .code(sku.getCode().getValue())
                .name(sku.getName())
                .description(sku.getDescription())
                .createdAt(sku.getCreatedAt());
        
        if (sku.getWeight() != null) {
            builder.weight(sku.getWeight().getValue())
                   .weightUnit(sku.getWeight().getUnit().name());
        }
        
        if (sku.getVolume() != null) {
            builder.volume(sku.getVolume().getValue())
                   .volumeUnit(sku.getVolume().getUnit().name());
        }
        
        return builder.build();
    }
}