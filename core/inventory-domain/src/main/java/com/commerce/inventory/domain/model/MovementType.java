package com.commerce.inventory.domain.model;

public enum MovementType {
    INBOUND("입고"),
    OUTBOUND("출고"),
    ADJUSTMENT("조정");
    
    private final String description;
    
    MovementType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}