package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuException;
import com.commerce.product.domain.model.AggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Sku extends AggregateRoot<SkuId> {
    
    private final SkuId id;
    private final SkuCode code;
    private String name;
    private String description;
    private Weight weight;
    private Volume volume;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Sku(CreateSkuCommand command) {
        validateCreate(command);
        
        this.id = command.getId();
        this.code = command.getCode();
        this.name = command.getName();
        this.description = command.getDescription();
        this.weight = command.getWeight();
        this.volume = command.getVolume();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    public static Sku create(CreateSkuCommand command) {
        return new Sku(command);
    }
    
    public void update(UpdateSkuCommand command) {
        if (command.getName() != null) {
            if (command.getName().trim().isEmpty()) {
                throw new InvalidSkuException("SKU 이름은 필수입니다");
            }
            this.name = command.getName();
        }
        
        if (command.getDescription() != null) {
            this.description = command.getDescription();
        }
        
        if (command.getWeight() != null) {
            this.weight = command.getWeight();
        }
        
        if (command.getVolume() != null) {
            this.volume = command.getVolume();
        }
        
        this.updatedAt = LocalDateTime.now();
    }
    
    private void validateCreate(CreateSkuCommand command) {
        if (command.getId() == null) {
            throw new InvalidSkuException("SKU ID는 필수입니다");
        }
        
        if (command.getCode() == null) {
            throw new InvalidSkuException("SKU 코드는 필수입니다");
        }
        
        if (command.getName() == null || command.getName().trim().isEmpty()) {
            throw new InvalidSkuException("SKU 이름은 필수입니다");
        }
    }
}