package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.usecase.ReceiveStockCommand;
import com.commerce.inventory.application.usecase.ReceiveStockUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.LoadSkuPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.application.port.out.SaveStockMovementPort;
import com.commerce.inventory.domain.exception.InvalidSkuException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.MovementType;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.inventory.domain.model.StockMovement;
import com.commerce.inventory.application.util.ValidationHelper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
    private final Validator validator;
    
    @Override
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void receive(ReceiveStockCommand command) {
        // Bean Validation을 사용한 유효성 검사
        ValidationHelper.validate(validator, command);
        
        SkuId skuId = SkuId.of(command.getSkuId());
        
        // SKU 존재 확인
        if (!loadSkuPort.exists(skuId)) {
            throw InvalidSkuException.notFound(skuId);
        }
        
        // 재고 조회 또는 생성
        Inventory inventory = loadInventoryPort.load(skuId)
            .orElseGet(() -> Inventory.createEmpty(skuId));
        
        // 재고 입고 처리
        Quantity quantity = Quantity.of(command.getQuantity());
        inventory.receive(quantity);
        
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