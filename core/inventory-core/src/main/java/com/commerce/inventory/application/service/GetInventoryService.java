package com.commerce.inventory.application.usecase;

import com.commerce.inventory.application.usecase.GetInventoryQuery;
import com.commerce.inventory.application.usecase.GetInventoryUseCase;
import com.commerce.inventory.application.usecase.InventoryResponse;
import com.commerce.inventory.application.service.port.out.LoadInventoryPort;
import com.commerce.inventory.application.util.ValidationHelper;
import com.commerce.inventory.domain.model.SkuId;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInventoryService implements GetInventoryUseCase {
    
    private final LoadInventoryPort loadInventoryPort;
    private final Validator validator;
    
    @Override
    public InventoryResponse execute(GetInventoryQuery query) {
        // null 입력에 대한 명시적인 검사
        if (query == null) {
            throw new IllegalArgumentException("GetInventoryQuery는 null일 수 없습니다");
        }
        
        // Bean Validation을 사용한 유효성 검사
        ValidationHelper.validate(validator, query);
        
        SkuId skuId = new SkuId(query.skuId());
        
        return loadInventoryPort.load(skuId)
            .map(InventoryResponse::from)
            .orElseGet(() -> InventoryResponse.empty(query.skuId()));
    }
}