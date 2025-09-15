package com.commerce.inventory.domain.model;

public enum ReservationStatus {
    ACTIVE("활성"),
    CONFIRMED("확정"),
    RELEASED("해제"),
    EXPIRED("만료");
    
    private final String description;
    
    ReservationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}