package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.ReceiveStockCommand;
import com.commerce.inventory.application.port.in.ReceiveStockUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.LoadSkuPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.application.port.out.SaveStockMovementPort;
import com.commerce.inventory.domain.exception.InvalidSkuException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.MovementType;
import com.commerce.inventory.domain.model.Sku;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.model.StockMovement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiveStockService implements ReceiveStockUseCase {
    
    private final LoadSkuPort loadSkuPort;
    private final LoadInventoryPort loadInventoryPort;
    private final SaveInventoryPort saveInventoryPort;
    private final SaveStockMovementPort saveStockMovementPort;
    private final Clock clock;
    
    @Override
    public void receive(ReceiveStockCommand command) {
        command.validate();
        
        SkuId skuId = SkuId.of(command.getSkuId());
        
        // SKU 존재 확인
        Sku sku = loadSkuPort.load(skuId)
            .orElseThrow(() -> new InvalidSkuException("존재하지 않는 SKU입니다: " + skuId.value()));
        
        // 재고 조회 또는 생성
        Inventory inventory = loadInventoryPort.load(skuId)
            .orElseGet(() -> Inventory.createEmpty(skuId));
        
        // 재고 입고 처리
        Quantity quantity = Quantity.of(command.getQuantity());
        inventory.receive(quantity, command.getReference());
        
        // 재고 저장
        saveInventoryPort.save(inventory);
        
        // 재고 이동 기록 생성 및 저장
        StockMovement movement = StockMovement.create(
            skuId,
            quantity,
            MovementType.RECEIVE,
            command.getReference(),
            LocalDateTime.now(clock)
        );
        saveStockMovementPort.save(movement);
    }
}