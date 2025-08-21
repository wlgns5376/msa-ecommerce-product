package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.infrastructure.persistence.entity.InventoryJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventoryPersistenceAdapter implements LoadInventoryPort, SaveInventoryPort {
    
    private static final int BATCH_SIZE = 1000;
    
    private final InventoryJpaRepository inventoryJpaRepository;
    
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
            return java.util.Collections.emptyMap();
        }
        
        Map<SkuId, Inventory> resultMap = new HashMap<>();
        List<String> skuIdValues = skuIds.stream()
                .map(SkuId::value)
                .collect(Collectors.toList());
        
        for (int i = 0; i < skuIdValues.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, skuIdValues.size());
            List<String> batch = skuIdValues.subList(i, endIndex);
            
            Map<SkuId, Inventory> batchResult = inventoryJpaRepository.findAllById(batch).stream()
                    .map(this::toDomainEntity)
                    .collect(Collectors.toMap(
                            Inventory::getSkuId,
                            java.util.function.Function.identity()
                    ));
            
            resultMap.putAll(batchResult);
        }
        
        return resultMap;
    }
    
    @Override
    @Transactional
    public void save(Inventory inventory) {
        try {
            InventoryJpaEntity entity = toJpaEntity(inventory);
            inventoryJpaRepository.save(entity);
        } catch (OptimisticLockException e) {
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
        } catch (OptimisticLockException e) {
            throw new OptimisticLockingFailureException(
                "동시성 충돌이 발생했습니다. 다시 시도해주세요. " + formatConflictingSkuIds(inventories),
                e
            );
        }
    }
    
    private String formatConflictingSkuIds(Collection<Inventory> inventories) {
        String ids = inventories.stream()
                .map(inv -> inv.getSkuId().value())
                .limit(10)
                .collect(Collectors.joining(", "));
        if (inventories.size() > 10) {
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