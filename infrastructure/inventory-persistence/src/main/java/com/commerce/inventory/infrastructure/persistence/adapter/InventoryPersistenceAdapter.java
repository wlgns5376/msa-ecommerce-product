package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.infrastructure.persistence.entity.InventoryJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventoryPersistenceAdapter implements LoadInventoryPort, SaveInventoryPort {
    
    private static final int MAX_SKU_IDS_IN_ERROR_MESSAGE = 10;
    
    @Value("${inventory.persistence.batch-size:1000}")
    private int batchSize;
    
    private final InventoryJpaRepository inventoryJpaRepository;
    
    @PostConstruct
    public void validateBatchSize() {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive.");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Inventory> load(SkuId skuId) {
        return inventoryJpaRepository.findById(skuId.value())
                .map(this::toDomainEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<SkuId, Inventory> loadAllByIds(List<SkuId> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }

        // 중복된 SKU ID를 제거하여 불필요한 작업을 줄입니다.
        List<SkuId> distinctSkuIds = skuIds.stream().distinct().collect(Collectors.toList());
        
        Map<SkuId, Inventory> resultMap = new HashMap<>(distinctSkuIds.size());
        
        for (int i = 0; i < distinctSkuIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, distinctSkuIds.size());
            List<String> batchIds = distinctSkuIds.subList(i, endIndex).stream()
                    .map(SkuId::value)
                    .collect(Collectors.toList());
            
            Map<SkuId, Inventory> batchResult = inventoryJpaRepository.findAllById(batchIds).stream()
                    .map(this::toDomainEntity)
                    .collect(Collectors.toMap(
                            Inventory::getSkuId,
                            inventory -> inventory
                    ));
            
            resultMap.putAll(batchResult);
        }
        
        return resultMap;
    }
    
    @Override
    @Transactional
    public Map<SkuId, Inventory> loadBySkuIdsWithLock(Set<SkuId> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }
        
        List<String> skuIdStrings = skuIds.stream()
                .map(SkuId::value)
                .collect(Collectors.toList());
        
        return inventoryJpaRepository.findAllByIdWithLock(skuIdStrings).stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toMap(
                        Inventory::getSkuId,
                        inventory -> inventory
                ));
    }
    
    @Override
    @Transactional
    public void save(Inventory inventory) {
        try {
            InventoryJpaEntity entity = toJpaEntity(inventory);
            inventoryJpaRepository.save(entity);
        } catch (OptimisticLockException | org.springframework.dao.OptimisticLockingFailureException e) {
            throw new OptimisticLockingFailureException(
                "동시성 충돌이 발생했습니다. 다시 시도해주세요. SKU ID: " + inventory.getSkuId().value(), 
                e
            );
        }
    }
    
    @Override
    @Transactional
    public void saveAll(Collection<Inventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return;
        }
        try {
            List<InventoryJpaEntity> entities = inventories.stream()
                .map(this::toJpaEntity)
                .collect(Collectors.toList());
            inventoryJpaRepository.saveAll(entities);
        } catch (OptimisticLockException | org.springframework.dao.OptimisticLockingFailureException e) {
            throw new OptimisticLockingFailureException(
                "동시성 충돌이 발생했습니다. 다시 시도해주세요. " + formatConflictingSkuIds(inventories),
                e
            );
        }
    }
    
    private String formatConflictingSkuIds(Collection<Inventory> inventories) {
        String ids = inventories.stream()
                .map(inv -> inv.getSkuId().value())
                .limit(MAX_SKU_IDS_IN_ERROR_MESSAGE)
                .collect(Collectors.joining(", "));
        if (inventories.size() > MAX_SKU_IDS_IN_ERROR_MESSAGE) {
            ids += " 등 (총 " + inventories.size() + "개)";
        }
        return "SKU IDs: " + ids;
    }
    
    private Inventory toDomainEntity(InventoryJpaEntity entity) {
        return Inventory.restore(
                SkuId.of(entity.getSkuId()),
                Quantity.of(entity.getTotalQuantity()),
                Quantity.of(entity.getReservedQuantity()),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
    
    private InventoryJpaEntity toJpaEntity(Inventory inventory) {
        return InventoryJpaEntity.builder()
                .skuId(inventory.getSkuId().value())
                .totalQuantity(inventory.getTotalQuantity().value())
                .reservedQuantity(inventory.getReservedQuantity().value())
                .version(inventory.getVersion())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}