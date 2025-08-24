package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class GetProductService implements GetProductUseCase {
    
    private final ProductRepository productRepository;
    private final StockAvailabilityService stockAvailabilityService;
    
    @Override
    public GetProductResponse execute(GetProductRequest request) {
        log.info("Getting product: {}", request.getProductId());
        
        ProductId productId = new ProductId(request.getProductId());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));
        
        List<GetProductResponse.ProductOptionResponse> optionResponses = new ArrayList<>();
        
        for (ProductOption option : product.getOptions()) {
            GetProductResponse.ProductOptionResponse optionResponse = buildOptionResponse(option, product.getType());
            optionResponses.add(optionResponse);
        }
        
        return GetProductResponse.builder()
            .productId(product.getId().value())
            .name(product.getName().value())
            .description(product.getDescription())
            .type(product.getType())
            .status(product.getStatus())
            .options(optionResponses)
            .build();
    }
    
    private GetProductResponse.ProductOptionResponse buildOptionResponse(ProductOption option, ProductType productType) {
        boolean isAvailable = false;
        int availableQuantity = 0;
        
        try {
            if (productType == ProductType.BUNDLE && option.isBundle()) {
                // 묶음 상품 재고 확인
                CompletableFuture<BundleAvailabilityResult> future = 
                    stockAvailabilityService.checkBundleAvailability(option.getSkuMapping());
                BundleAvailabilityResult result = future.get(5, TimeUnit.SECONDS);
                isAvailable = result.isAvailable();
                availableQuantity = result.availableSets();
            } else {
                // 일반 상품 재고 확인
                CompletableFuture<AvailabilityResult> future = 
                    stockAvailabilityService.checkProductOptionAvailability(option.getId());
                AvailabilityResult result = future.get(5, TimeUnit.SECONDS);
                isAvailable = result.isAvailable();
                availableQuantity = result.availableQuantity();
            }
        } catch (Exception e) {
            log.error("Failed to check stock availability for option: {}", option.getId(), e);
            // 재고 확인 실패 시 기본값 유지 (재고 없음)
        }
        
        List<GetProductResponse.SkuMappingResponse> skuMappingResponses = option.getSkuMapping().mappings().entrySet().stream()
            .map(entry -> GetProductResponse.SkuMappingResponse.builder()
                .skuId(entry.getKey())
                .quantity(entry.getValue())
                .build())
            .collect(Collectors.toList());
        
        return GetProductResponse.ProductOptionResponse.builder()
            .optionId(option.getId())
            .name(option.getName())
            .price(option.getPrice().amount().longValue())
            .currency(option.getPrice().currency().name())
            .skuMappings(skuMappingResponses)
            .isAvailable(isAvailable)
            .availableQuantity(availableQuantity)
            .build();
    }
}