package com.commerce.inventory.domain.model;

public enum MovementType {
    RECEIVE("재고 입고"),  // 구매 주문에 의한 재고 입고
    INBOUND("입고 예정"),  // 입고 예정 재고 (추후 확장을 위해 예약)
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