package com.commerce.product.domain.service.impl;

import com.commerce.product.domain.model.ProductOption;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class StockAvailabilityServiceImpl implements StockAvailabilityService {
    
    private final InventoryRepository inventoryRepository;
    private final LockRepository lockRepository;
    private static final long LOCK_TIMEOUT_MILLIS = 5000L;
    
    @Override
    public boolean checkSingleOption(String skuId, int requestedQuantity) {
        if (requestedQuantity < 0) {
            log.warn("Invalid requested quantity: {} for SKU: {}", requestedQuantity, skuId);
            return false;
        }
        
        if (requestedQuantity == 0) {
            return true;
        }
        
        int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
        
        if (availableQuantity < 0) {
            log.warn("Negative available quantity: {} for SKU: {}", availableQuantity, skuId);
            return false;
        }
        
        boolean isAvailable = availableQuantity >= requestedQuantity;
        
        if (!isAvailable) {
            log.debug("Insufficient stock for SKU: {}. Available: {}, Requested: {}", 
                    skuId, availableQuantity, requestedQuantity);
        }
        
        return isAvailable;
    }
    
    @Override
    public boolean checkBundleOption(ProductOption bundleOption, int requestedQuantity) {
        if (!bundleOption.isBundle()) {
            return checkSingleOption(bundleOption.getSingleSkuId(), requestedQuantity);
        }
        
        for (Map.Entry<String, Integer> entry : bundleOption.getSkuMapping().mappings().entrySet()) {
            String skuId = entry.getKey();
            int requiredQuantity = entry.getValue() * requestedQuantity;
            
            if (!checkSingleOption(skuId, requiredQuantity)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public CompletableFuture<Map<String, Boolean>> checkMultipleOptions(
            List<ProductOption> options, Map<String, Integer> quantities) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Boolean> results = new ConcurrentHashMap<>();
            
            List<CompletableFuture<Void>> futures = options.stream()
                    .map(option -> CompletableFuture.runAsync(() -> {
                        String optionName = option.getName();
                        Integer requestedQuantity = quantities.get(optionName);
                        
                        if (requestedQuantity == null) {
                            results.put(optionName, false);
                            return;
                        }
                        
                        boolean available = option.isBundle() 
                                ? checkBundleOption(option, requestedQuantity)
                                : checkSingleOption(option.getSingleSkuId(), requestedQuantity);
                        
                        results.put(optionName, available);
                    }))
                    .collect(Collectors.toList());
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            return results;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> reserveStock(String skuId, int quantity, String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            Lock lock = lockRepository.acquireLock("stock:" + skuId, LOCK_TIMEOUT_MILLIS);
            
            if (lock == null) {
                log.warn("Failed to acquire lock for SKU: {}", skuId);
                return false;
            }
            
            try {
                int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
                
                if (availableQuantity < quantity) {
                    log.info("Insufficient stock for SKU: {}. Available: {}, Requested: {}", 
                            skuId, availableQuantity, quantity);
                    return false;
                }
                
                String reservationId = inventoryRepository.reserveStock(skuId, quantity, orderId);
                log.info("Stock reserved. SKU: {}, Quantity: {}, Order: {}, Reservation: {}", 
                        skuId, quantity, orderId, reservationId);
                
                return true;
                
            } finally {
                lockRepository.releaseLock(lock);
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> reserveBundleStock(
            ProductOption bundleOption, int quantity, String orderId) {
        
        if (!bundleOption.isBundle()) {
            return reserveStock(bundleOption.getSingleSkuId(), quantity, orderId);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            
            Map<String, Integer> requiredQuantities = new HashMap<>();
            List<String> skuIds = new ArrayList<>();
            
            for (Map.Entry<String, Integer> entry : bundleOption.getSkuMapping().mappings().entrySet()) {
                String skuId = entry.getKey();
                int requiredQuantity = entry.getValue() * quantity;
                requiredQuantities.put(skuId, requiredQuantity);
                skuIds.add(skuId);
            }
            
            // 데드락 방지를 위해 SKU ID 순서대로 정렬
            Collections.sort(skuIds);
            
            Map<String, Lock> locks = new LinkedHashMap<>();
            Map<String, String> reservations = new LinkedHashMap<>();
            
            try {
                // 모든 락 획득
                for (String skuId : skuIds) {
                    Lock lock = lockRepository.acquireLock("stock:" + skuId, LOCK_TIMEOUT_MILLIS);
                    if (lock == null) {
                        log.warn("Failed to acquire lock for SKU: {} in bundle", skuId);
                        return false;
                    }
                    locks.put(skuId, lock);
                }
                
                // 재고 가용성 체크
                for (String skuId : skuIds) {
                    int availableQuantity = inventoryRepository.getAvailableQuantity(skuId);
                    int requiredQuantity = requiredQuantities.get(skuId);
                    
                    if (availableQuantity < requiredQuantity) {
                        log.info("Insufficient stock for bundle SKU: {}. Available: {}, Required: {}", 
                                skuId, availableQuantity, requiredQuantity);
                        // 보상 트랜잭션 실행
                        compensateReservations(reservations);
                        return false;
                    }
                }
                
                // 모든 SKU 예약
                for (String skuId : skuIds) {
                    int requiredQuantity = requiredQuantities.get(skuId);
                    String reservationId = inventoryRepository.reserveStock(skuId, requiredQuantity, orderId);
                    reservations.put(skuId, reservationId);
                    
                    log.info("Bundle stock reserved. SKU: {}, Quantity: {}, Order: {}, Reservation: {}", 
                            skuId, requiredQuantity, orderId, reservationId);
                }
                
                return true;
                
            } catch (Exception e) {
                log.error("Error during bundle stock reservation", e);
                compensateReservations(reservations);
                return false;
                
            } finally {
                // 역순으로 락 해제
                List<String> reverseSkuIds = new ArrayList<>(locks.keySet());
                Collections.reverse(reverseSkuIds);
                
                for (String skuId : reverseSkuIds) {
                    Lock lock = locks.get(skuId);
                    if (lock != null) {
                        lockRepository.releaseLock(lock);
                    }
                }
            }
        });
    }
    
    private void compensateReservations(Map<String, String> reservations) {
        for (Map.Entry<String, String> entry : reservations.entrySet()) {
            String skuId = entry.getKey();
            String reservationId = entry.getValue();
            
            try {
                inventoryRepository.releaseReservation(reservationId);
                log.info("Compensated reservation. SKU: {}, Reservation: {}", skuId, reservationId);
            } catch (Exception e) {
                log.error("Failed to compensate reservation. SKU: {}, Reservation: {}", 
                        skuId, reservationId, e);
            }
        }
    }
    
    @Override
    public void releaseReservation(String reservationId) {
        inventoryRepository.releaseReservation(reservationId);
        log.info("Released reservation: {}", reservationId);
    }
    
    @Override
    public int getAvailableQuantity(String skuId) {
        return inventoryRepository.getAvailableQuantity(skuId);
    }
    
    @Override
    public Map<String, Integer> getBundleAvailableQuantity(ProductOption bundleOption) {
        Map<String, Integer> quantities = new HashMap<>();
        
        for (String skuId : bundleOption.getSkuMapping().mappings().keySet()) {
            quantities.put(skuId, getAvailableQuantity(skuId));
        }
        
        return quantities;
    }
}