package com.commerce.inventory.application.usecase;

import com.commerce.inventory.application.port.in.GetInventoryQuery;
import com.commerce.inventory.application.port.in.GetInventoryUseCase;
import com.commerce.inventory.application.port.in.InventoryResponse;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.domain.model.SkuId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class GetInventoryService implements GetInventoryUseCase {
    
    private final LoadInventoryPort loadInventoryPort;
    
    public GetInventoryService(LoadInventoryPort loadInventoryPort) {
        this.loadInventoryPort = loadInventoryPort;
    }
    
    @Override
    public InventoryResponse execute(@Valid @NotNull(message = "GetInventoryQuery cannot be null.") GetInventoryQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("GetInventoryQuery cannot be null.");
        }
        
        SkuId skuId = new SkuId(query.skuId());
        
        return loadInventoryPort.load(skuId)
            .map(InventoryResponse::from)
            .orElseGet(() -> InventoryResponse.empty(query.skuId()));
    }
}