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
    public GetProductResponse getProduct(GetProductQuery query) {
        log.info("Getting product: {}", query.getProductId());
        
        ProductId productId = ProductId.of(query.getProductId());
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + query.getProductId()));
        
        // 모든 옵션에 대한 CompletableFuture 수집
        Map<String, CompletableFuture<Availability>> futures = product.getOptions().stream()
            .collect(Collectors.toMap(
                ProductOption::getId,
                option -> getAvailabilityFuture(option).exceptionally(ex -> {
                    log.error("Failed to get stock availability for option: {}", option.getId(), ex);
                    return new Availability(false, 0); // 예외 발생 시 기본값 반환
                })
            ));
        
        // 모든 Future가 완료될 때까지 대기
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.values().toArray(CompletableFuture[]::new)
        );
        
        try {
            allFutures.get(stockAvailabilityTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Timed out while checking stock availability for all options", e);
        } catch (InterruptedException e) {
            log.warn("Stock availability check interrupted. Proceeding with default availability.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // 예상치 못한 예외가 발생한 경우
            log.error("Unexpected error while checking stock availability", e);
        }
        
        // 결과를 기반으로 응답 생성
        List<GetProductResponse.OptionDetail> optionResponses = product.getOptions().stream()
            .map(option -> buildOptionResponse(option, futures.get(option.getId())))
            .collect(Collectors.toList());
        
        return GetProductResponse.builder()
            .id(product.getId().toString())
            .name(product.getName().value())
            .description(product.getDescription())
            .type(product.getType().name())
            .status(product.getStatus().name())
            .options(optionResponses)
            .build();
    }
    
    private CompletableFuture<Availability> getAvailabilityFuture(ProductOption option) {
        if (option.isBundle()) {
            return stockAvailabilityService.checkBundleAvailability(option.getSkuMapping())
                .thenApply(r -> new Availability(r.isAvailable(), r.availableSets()));
        } else {
            // N+1 문제를 방지하기 위해 SKU ID를 직접 사용
            String skuId = option.getSingleSkuId();
            return stockAvailabilityService.checkSingleSkuAvailability(skuId)
                .thenApply(r -> new Availability(r.isAvailable(), r.availableQuantity()));
        }
    }
    
    private GetProductResponse.OptionDetail buildOptionResponse(ProductOption option, CompletableFuture<Availability> future) {
        Availability availability = future.getNow(new Availability(false, 0));
        
        List<GetProductResponse.SkuMappingDetail> skuMappingResponses = option.getSkuMapping().mappings().entrySet().stream()
            .map(entry -> GetProductResponse.SkuMappingDetail.of(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        
        return GetProductResponse.OptionDetail.builder()
            .id(option.getId())
            .name(option.getName())
            .price(option.getPrice().amount())
            .currency(option.getPrice().currency().name())
            .skuMappings(skuMappingResponses)
            .build();
    }
    
}
