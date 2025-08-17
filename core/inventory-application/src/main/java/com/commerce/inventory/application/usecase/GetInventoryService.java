package com.commerce.inventory.application.usecase;

import com.commerce.inventory.application.port.in.GetInventoryQuery;
import com.commerce.inventory.application.port.in.GetInventoryUseCase;
import com.commerce.inventory.application.port.in.InventoryResponse;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
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
        // Bean Validation을 사용한 유효성 검사
        ValidationHelper.validate(validator, query);
        
        SkuId skuId = new SkuId(query.skuId());
        
        return loadInventoryPort.load(skuId)
            .map(InventoryResponse::from)
            .orElseGet(() -> InventoryResponse.empty(query.skuId()));
    }
}