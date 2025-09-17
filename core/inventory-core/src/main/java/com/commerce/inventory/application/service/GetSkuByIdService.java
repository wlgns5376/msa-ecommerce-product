package com.commerce.inventory.application.service;

import com.commerce.inventory.application.usecase.GetSkuByIdQuery;
import com.commerce.inventory.application.usecase.GetSkuByIdResponse;
import com.commerce.inventory.application.usecase.GetSkuByIdUseCase;
import com.commerce.inventory.domain.exception.SkuNotFoundException;
import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSkuByIdService implements GetSkuByIdUseCase {
    
    private final SkuRepository skuRepository;
    
    @Override
    public GetSkuByIdResponse execute(GetSkuByIdQuery query) {
        SkuId skuId = SkuId.of(query.getSkuId());
        
        Sku sku = skuRepository.findById(skuId)
            .orElseThrow(() -> new SkuNotFoundException("SKU를 찾을 수 없습니다. ID: " + query.getSkuId()));
        
        return GetSkuByIdResponse.from(sku);
    }
}