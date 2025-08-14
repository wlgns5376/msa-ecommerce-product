package com.commerce.inventory.domain.application.usecase;

import com.commerce.inventory.domain.exception.DuplicateSkuCodeException;
import com.commerce.inventory.domain.exception.InvalidSkuCodeException;
import com.commerce.inventory.domain.exception.InvalidVolumeException;
import com.commerce.inventory.domain.exception.InvalidWeightException;
import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.domain.repository.SkuRepository;
import com.commerce.common.application.usecase.UseCase;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class CreateSkuUseCase implements UseCase<CreateSkuRequest, CreateSkuResponse> {
    
    private final SkuRepository skuRepository;
    private final Clock clock;
    
    @Override
    public CreateSkuResponse execute(CreateSkuRequest request) {
        SkuCode skuCode = SkuCode.of(request.getCode());
        
        checkDuplicateCode(skuCode);
        
        CreateSkuCommand command = buildCreateCommand(request, skuCode);
        
        Sku sku = Sku.create(command, LocalDateTime.now(clock));
        
        Sku savedSku = skuRepository.save(sku);
        
        return mapToResponse(savedSku);
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
        
        boolean weightValueProvided = request.getWeight() != null;
        boolean weightUnitProvided = request.getWeightUnit() != null;
        if (weightValueProvided != weightUnitProvided) {
            throw new InvalidWeightException("무게와 무게 단위는 모두 제공되거나 모두 제공되지 않아야 합니다.");
        }
        if (weightValueProvided) {
            WeightUnit weightUnit = WeightUnit.fromString(request.getWeightUnit());
            builder.weight(Weight.of(request.getWeight(), weightUnit));
        }

        boolean volumeValueProvided = request.getVolume() != null;
        boolean volumeUnitProvided = request.getVolumeUnit() != null;
        if (volumeValueProvided != volumeUnitProvided) {
            throw new InvalidVolumeException("부피와 부피 단위는 모두 제공되거나 모두 제공되지 않아야 합니다.");
        }
        if (volumeValueProvided) {
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