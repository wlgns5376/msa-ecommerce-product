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
        SkuCode skuCode = createSkuCode(request.getCode());
        
        checkDuplicateCode(skuCode);
        
        CreateSkuCommand command = buildCreateCommand(request, skuCode);
        
        Sku sku = Sku.create(command, LocalDateTime.now());
        
        Sku savedSku = skuRepository.save(sku);
        
        return mapToResponse(savedSku);
    }
    
    private SkuCode createSkuCode(String code) {
        return SkuCode.of(code);
    }
    
    private void checkDuplicateCode(SkuCode skuCode) {
        skuRepository.findByCode(skuCode)
                .ifPresent(sku -> {
                    throw new DuplicateSkuCodeException("이미 존재하는 SKU 코드입니다: " + skuCode.value());
                });
    }
    
    private CreateSkuCommand buildCreateCommand(CreateSkuRequest request, SkuCode skuCode) {
        CreateSkuCommand.CreateSkuCommandBuilder builder = CreateSkuCommand.builder()
                .id(SkuId.generate())
                .code(skuCode)
                .name(request.getName())
                .description(request.getDescription());
        
        if (request.getWeight() != null && request.getWeightUnit() != null) {
            WeightUnit weightUnit = WeightUnit.fromString(request.getWeightUnit());
            builder.weight(Weight.of(request.getWeight(), weightUnit));
        }
        
        if (request.getVolume() != null && request.getVolumeUnit() != null) {
            VolumeUnit volumeUnit = VolumeUnit.fromString(request.getVolumeUnit());
            builder.volume(Volume.of(request.getVolume(), volumeUnit));
        }
        
        return builder.build();
    }
    
    private CreateSkuResponse mapToResponse(Sku sku) {
        CreateSkuResponse.CreateSkuResponseBuilder builder = CreateSkuResponse.builder()
                .id(sku.getId().value())
                .code(sku.getCode().value())
                .name(sku.getName())
                .description(sku.getDescription())
                .createdAt(sku.getCreatedAt());
        
        if (sku.getWeight() != null) {
            builder.weight(sku.getWeight().value())
                   .weightUnit(sku.getWeight().unit().name());
        }
        
        if (sku.getVolume() != null) {
            builder.volume(sku.getVolume().value())
                   .volumeUnit(sku.getVolume().unit().name());
        }
        
        return builder.build();
    }
}