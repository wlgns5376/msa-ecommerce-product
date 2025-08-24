package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class GetProductService implements GetProductUseCase {
    
    private final ProductRepository productRepository;
    private final StockAvailabilityService stockAvailabilityService;
    
    @Value("${stock.availability.timeout.seconds:5}")
    private long stockAvailabilityTimeoutSeconds;
    
    @Override
    public GetProductResponse execute(GetProductRequest request) {
        log.info("Getting product: {}", request.getProductId());
        
        ProductId productId = ProductId.of(request.getProductId());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));
        
        // 모든 옵션에 대한 CompletableFuture 수집
        Map<ProductOption, CompletableFuture<?>> futures = product.getOptions().stream()
            .collect(Collectors.toMap(
                option -> option,
                option -> getAvailabilityFuture(option, product.getType())
            ));
        
        // 모든 Future가 완료될 때까지 대기
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.values().toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(stockAvailabilityTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Failed to check stock availability for all options", e);
        }
        
        // 결과를 기반으로 응답 생성
        List<GetProductResponse.ProductOptionResponse> optionResponses = product.getOptions().stream()
            .map(option -> buildOptionResponse(option, futures.get(option)))
            .collect(Collectors.toList());
        
        return GetProductResponse.builder()
            .productId(product.getId().value())
            .name(product.getName().value())
            .description(product.getDescription())
            .type(product.getType())
            .status(product.getStatus())
            .options(optionResponses)
            .build();
    }
    
    private CompletableFuture<?> getAvailabilityFuture(ProductOption option, ProductType productType) {
        if (productType == ProductType.BUNDLE && option.isBundle()) {
            return stockAvailabilityService.checkBundleAvailability(option.getSkuMapping());
        } else {
            return stockAvailabilityService.checkProductOptionAvailability(option.getId());
        }
    }
    
    private GetProductResponse.ProductOptionResponse buildOptionResponse(ProductOption option, CompletableFuture<?> future) {
        boolean isAvailable = false;
        int availableQuantity = 0;
        
        try {
            if (future != null && future.isDone()) {
                Object result = future.get();
                if (result instanceof BundleAvailabilityResult bundleResult) {
                    isAvailable = bundleResult.isAvailable();
                    availableQuantity = bundleResult.availableSets();
                } else if (result instanceof AvailabilityResult availabilityResult) {
                    isAvailable = availabilityResult.isAvailable();
                    availableQuantity = availabilityResult.availableQuantity();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to get stock availability result for option: {}", option.getId(), e);
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
            .price(option.getPrice().amount())
            .currency(option.getPrice().currency().name())
            .skuMappings(skuMappingResponses)
            .isAvailable(isAvailable)
            .availableQuantity(availableQuantity)
            .build();
    }
}