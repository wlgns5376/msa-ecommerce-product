package com.commerce.inventory.application.usecase;

import com.commerce.inventory.domain.application.usecase.CreateSkuCommand;
import com.commerce.inventory.domain.application.usecase.CreateSkuResponse;
import com.commerce.inventory.domain.application.usecase.CreateSkuUseCase;
import com.commerce.inventory.application.port.out.LoadSkuPort;
import com.commerce.inventory.application.port.out.SaveSkuPort;
import com.commerce.inventory.domain.exception.DuplicateSkuCodeException;
import com.commerce.inventory.domain.exception.InvalidVolumeException;
import com.commerce.inventory.domain.exception.InvalidWeightException;
import com.commerce.inventory.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateSkuService implements CreateSkuUseCase {
    
    private final LoadSkuPort loadSkuPort;
    private final SaveSkuPort saveSkuPort;
    private final Clock clock;
    
    @Override
    public CreateSkuResponse execute(CreateSkuCommand request) {
        SkuCode skuCode = SkuCode.of(request.getCode());
        
        // 빠른 실패(fast-fail)를 위한 사전 검사
        if (loadSkuPort.existsByCode(skuCode)) {
            throw new DuplicateSkuCodeException("이미 존재하는 SKU 코드입니다: " + skuCode.value());
        }
        
        SkuCreationData command = buildCreateCommand(request, skuCode);
        Sku sku = Sku.create(command, LocalDateTime.now(clock));
        
        try {
            Sku savedSku = saveSkuPort.save(sku);
            return mapToResponse(savedSku);
        } catch (DataIntegrityViolationException e) {
            // 데이터베이스 제약 조건 위반으로 인한 예외 처리
            throw new DuplicateSkuCodeException("SKU 코드 '" + skuCode.value() + "'가 이미 존재하여 생성에 실패했습니다.", e);
        }
    }
    
    private SkuCreationData buildCreateCommand(CreateSkuCommand request, SkuCode skuCode) {
        SkuCreationData.SkuCreationDataBuilder builder = SkuCreationData.builder()
                .id(SkuId.generate())
                .code(skuCode)
                .name(request.getName())
                .description(request.getDescription());
        
        addWeightToCommand(builder, request);
        addVolumeToCommand(builder, request);
        
        return builder.build();
    }
    
    private void addWeightToCommand(SkuCreationData.SkuCreationDataBuilder builder, CreateSkuCommand request) {
        boolean weightValueProvided = request.getWeight() != null;
        boolean weightUnitProvided = request.getWeightUnit() != null && !request.getWeightUnit().trim().isEmpty();
        if (weightValueProvided != weightUnitProvided) {
            throw new InvalidWeightException("무게와 무게 단위는 모두 제공되거나 모두 제공되지 않아야 합니다.");
        }
        if (weightValueProvided) {
            WeightUnit weightUnit = WeightUnit.fromString(request.getWeightUnit());
            builder.weight(Weight.of(request.getWeight(), weightUnit));
        }
    }
    
    private void addVolumeToCommand(SkuCreationData.SkuCreationDataBuilder builder, CreateSkuCommand request) {
        boolean volumeValueProvided = request.getVolume() != null;
        boolean volumeUnitProvided = request.getVolumeUnit() != null && !request.getVolumeUnit().trim().isEmpty();
        if (volumeValueProvided != volumeUnitProvided) {
            throw new InvalidVolumeException("부피와 부피 단위는 모두 제공되거나 모두 제공되지 않아야 합니다.");
        }
        if (volumeValueProvided) {
            VolumeUnit volumeUnit = VolumeUnit.fromString(request.getVolumeUnit());
            builder.volume(Volume.of(request.getVolume(), volumeUnit));
        }
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