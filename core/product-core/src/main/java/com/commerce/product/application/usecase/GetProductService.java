package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.ProductNotFoundException;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;
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
@Service
public class GetProductService implements GetProductUseCase {
    
    // 가용성 정보를 담는 내부 record
    private record Availability(boolean isAvailable, int availableQuantity) {}
    
    private final ProductRepository productRepository;
    private final StockAvailabilityService stockAvailabilityService;
    private final long stockAvailabilityTimeoutSeconds;
    
    public GetProductService(ProductRepository productRepository,
                           StockAvailabilityService stockAvailabilityService,
                           @Value("${stock.availability.timeout.seconds:5}") long stockAvailabilityTimeoutSeconds) {
        this.productRepository = productRepository;
        this.stockAvailabilityService = stockAvailabilityService;
        this.stockAvailabilityTimeoutSeconds = stockAvailabilityTimeoutSeconds;
    }
    
    @Override
    public GetProductResponse execute(GetProductRequest request) {
        log.info("Getting product: {}", request.getProductId());
        
        ProductId productId = ProductId.of(request.getProductId());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + request.getProductId()));
        
        // 모든 옵션에 대한 CompletableFuture 수집
        Map<ProductOption, CompletableFuture<?>> futures = product.getOptions().stream()
            .collect(Collectors.toMap(
                option -> option,
                option -> getAvailabilityFuture(option)
            ));
        
        // 모든 Future가 완료될 때까지 대기
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.values().toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(stockAvailabilityTimeoutSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to check stock availability for all options", e);
        } catch (InterruptedException e) {
            log.error("Stock availability check interrupted", e);
            Thread.currentThread().interrupt();
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
    
    private CompletableFuture<?> getAvailabilityFuture(ProductOption option) {
        if (option.isBundle()) {
            return stockAvailabilityService.checkBundleAvailability(option.getSkuMapping());
        } else {
            return stockAvailabilityService.checkProductOptionAvailability(option.getId());
        }
    }
    
    private GetProductResponse.ProductOptionResponse buildOptionResponse(ProductOption option, CompletableFuture<?> future) {
        Availability availability = new Availability(false, 0);
        
        try {
            if (future != null && future.isDone()) {
                Object result = future.get();
                availability = toAvailability(result);
            }
        } catch (ExecutionException e) {
            log.error("Failed to get stock availability result for option: {}", option.getId(), e);
            // 재고 확인 실패 시 기본값 유지 (재고 없음)
        } catch (InterruptedException e) {
            log.error("Interrupted while getting stock availability result for option: {}", option.getId(), e);
            Thread.currentThread().interrupt();
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
            .isAvailable(availability.isAvailable())
            .availableQuantity(availability.availableQuantity())
            .build();
    }
    
    private Availability toAvailability(Object result) {
        if (result instanceof BundleAvailabilityResult bundleResult) {
            return new Availability(bundleResult.isAvailable(), bundleResult.availableSets());
        }
        if (result instanceof AvailabilityResult availabilityResult) {
            return new Availability(availabilityResult.isAvailable(), availabilityResult.availableQuantity());
        }
        return new Availability(false, 0);
    }
}