package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuException;
import com.commerce.common.domain.model.AggregateRoot;
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
    private Long version;
    
    private Sku(CreateSkuCommand command, LocalDateTime currentTime) {
        validateCreate(command);
        
        this.id = command.getId();
        this.code = command.getCode();
        this.name = command.getName();
        this.description = command.getDescription();
        this.weight = command.getWeight();
        this.volume = command.getVolume();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.version = 0L;
    }
    
    public static Sku create(CreateSkuCommand command, LocalDateTime currentTime) {
        return new Sku(command, currentTime);
    }
    
    public static Sku create(SkuId id, SkuCode code, String name, Weight weight, Volume volume, LocalDateTime currentTime) {
        CreateSkuCommand command = CreateSkuCommand.builder()
            .id(id)
            .code(code)
            .name(name)
            .weight(weight)
            .volume(volume)
            .build();
        return new Sku(command, currentTime);
    }
    
    public static Sku restore(
            SkuId id,
            SkuCode code,
            String name,
            String description,
            Weight weight,
            Volume volume,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        Sku sku = new Sku(
                CreateSkuCommand.builder()
                        .id(id)
                        .code(code)
                        .name(name)
                        .description(description)
                        .weight(weight)
                        .volume(volume)
                        .build(),
                createdAt
        );
        sku.updatedAt = updatedAt;
        sku.version = version;
        return sku;
    }
    
    public void update(UpdateSkuCommand command, LocalDateTime currentTime) {
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
        
        this.updatedAt = currentTime;
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
    
    @Override
    public SkuId getId() {
        return id;
    }
    
    public Long getVersion() {
        return version;
    }
}