package com.commerce.product.domain.model.saga;

public enum SagaStatus {
    STARTED("Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    COMPENSATING("Compensating"),
    COMPENSATED("Compensated"),
    FAILED("Failed");
    
    private final String description;
    
    SagaStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == COMPENSATED || this == FAILED;
    }
    
    public boolean canCompensate() {
        return this == IN_PROGRESS || this == COMPENSATING;
    }
}