package com.commerce.product.domain.service.impl;

import com.commerce.product.domain.exception.LockAcquisitionException;
import com.commerce.product.domain.model.inventory.Inventory;
import com.commerce.product.domain.model.inventory.SkuId;
import com.commerce.product.domain.model.ProductOption;
import com.commerce.product.domain.model.SkuMapping;
import com.commerce.product.domain.repository.InventoryRepositoryV2;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.StockAvailabilityServiceV2;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class StockAvailabilityServiceV2Impl implements StockAvailabilityServiceV2 {
    
    private final InventoryRepositoryV2 inventoryRepository;
    private final ProductRepository productRepository;
    private final LockRepository lockRepository;
    
    private static final long LOCK_TIMEOUT_MILLIS = 5000L;
    
    @Override
    public CompletableFuture<AvailabilityResult> checkProductOptionAvailability(String optionId) {
        log.debug("Checking availability for option: {}", optionId);

        return CompletableFuture.supplyAsync(() -> productRepository.findOptionById(optionId))
                .thenCompose(optionOpt -> {
                    if (optionOpt.isEmpty()) {
                        log.warn("Option not found: {}", optionId);
                        return CompletableFuture.completedFuture(AvailabilityResult.unavailable());
                    }

                    ProductOption option = optionOpt.get();
                    SkuMapping skuMapping = option.getSkuMapping();

                    if (!skuMapping.isBundle()) {
                        // 단일 SKU 옵션
                        return CompletableFuture.supplyAsync(() -> {
                            String skuId = skuMapping.getSingleSkuId();
                            return inventoryRepository.findBySkuId(SkuId.of(skuId))
                                    .map(inventory -> {
                                        int availableQuantity = inventory.getAvailableQuantity().getValue();
                                        return availableQuantity > 0
                                                ? AvailabilityResult.available(availableQuantity)
                                                : AvailabilityResult.unavailable();
                                    })
                                    .orElseGet(() -> {
                                        log.warn("Inventory not found for SKU: {}", skuId);
                                        return AvailabilityResult.unavailable();
                                    });
                        });
                    } else {
                        // 묶음 옵션은 checkBundleAvailability 사용
                        return checkBundleAvailability(skuMapping)
                                .thenApply(bundleResult -> bundleResult.isAvailable()
                                        ? AvailabilityResult.available(bundleResult.availableSets())
                                        : AvailabilityResult.unavailable());
                    }
                }).exceptionally(e -> {
                    log.error("Error checking product option availability for optionId: {}", optionId, e);
                    return AvailabilityResult.unavailable();
                });
    }
    
    @Override
    public CompletableFuture<BundleAvailabilityResult> checkBundleAvailability(SkuMapping skuMapping) {
        return CompletableFuture.supplyAsync(() -> {
            // 분산 락을 사용하여 번들 재고 확인의 원자성 보장
            String lockKey = generateLockKey(skuMapping);
            Lock lock = lockRepository.acquireLock(lockKey, LOCK_TIMEOUT_MILLIS);
            
            if (lock == null) {
                log.error("Failed to acquire lock for key: {}", lockKey);
                throw new LockAcquisitionException("Failed to acquire lock for bundle availability check");
            }
            
            log.debug("Acquired lock for bundle availability check: {}", lockKey);
            
            try {
                // 일괄 조회로 N+1 쿼리 문제 해결
                Map<SkuId, Inventory> inventoryMap = inventoryRepository.findBySkuIds(
                    skuMapping.mappings().keySet().stream().map(SkuId::of).collect(Collectors.toList())
                );

                List<BundleAvailabilityResult.SkuAvailabilityDetail> details = new ArrayList<>();
                int minAvailableSets = Integer.MAX_VALUE;
                
                for (Map.Entry<String, Integer> entry : skuMapping.mappings().entrySet()) {
                    String skuId = entry.getKey();
                    int requiredQuantity = entry.getValue();
                    
                    Inventory inventory = inventoryMap.get(SkuId.of(skuId));
                    
                    if (inventory == null) {
                        log.warn("Inventory not found for SKU: {}", skuId);
                        details.add(new BundleAvailabilityResult.SkuAvailabilityDetail(
                            skuId, requiredQuantity, 0, 0
                        ));
                        minAvailableSets = 0;
                        continue;
                    }
                    
                    int availableQuantity = inventory.getAvailableQuantity().getValue();
                    int availableSets = availableQuantity / requiredQuantity;
                    
                    details.add(new BundleAvailabilityResult.SkuAvailabilityDetail(
                        skuId, requiredQuantity, availableQuantity, availableSets
                    ));
                    
                    minAvailableSets = Math.min(minAvailableSets, availableSets);
                    
                    log.debug("SKU {} - Required: {}, Available: {}, Sets: {}", 
                        skuId, requiredQuantity, availableQuantity, availableSets);
                }
                
                log.info("Bundle availability check result - Available sets: {}", minAvailableSets);
                
                return minAvailableSets > 0
                    ? BundleAvailabilityResult.available(minAvailableSets, details)
                    : BundleAvailabilityResult.unavailable(details);
                    
            } finally {
                lockRepository.releaseLock(lock);
                log.debug("Released lock for bundle availability check: {}", lockKey);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> reserveBundleStock(SkuMapping skuMapping, String reservationId) {
        // TODO: Saga 패턴을 사용한 분산 트랜잭션 구현
        // 이 부분은 별도의 작업으로 구현 예정
        return CompletableFuture.completedFuture(null);
    }
    
    private String generateLockKey(SkuMapping skuMapping) {
        // 데드락 방지를 위해 SKU ID를 정렬
        String sortedSkuIds = skuMapping.mappings().keySet().stream()
            .sorted()
            .collect(Collectors.joining(":"));
        return "bundle-stock-check:" + sortedSkuIds;
    }
}