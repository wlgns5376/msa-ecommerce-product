package com.commerce.product.domain.model.saga;

import java.time.LocalDateTime;

public class SagaStep {
    private final String stepId;
    private final String description;
    private final StepStatus status;
    private final LocalDateTime executedAt;
    private final String compensationData;
    private final String errorMessage;
    
    public enum StepStatus {
        PENDING,
        EXECUTING,
        COMPLETED,
        COMPENSATED,
        FAILED
    }
    
    private SagaStep(String stepId, String description, StepStatus status, 
                    LocalDateTime executedAt, String compensationData, String errorMessage) {
        this.stepId = stepId;
        this.description = description;
        this.status = status;
        this.executedAt = executedAt;
        this.compensationData = compensationData;
        this.errorMessage = errorMessage;
    }
    
    public static SagaStep pending(String stepId, String description) {
        return new SagaStep(stepId, description, StepStatus.PENDING, 
                LocalDateTime.now(), null, null);
    }
    
    public SagaStep complete(String compensationData) {
        return new SagaStep(this.stepId, this.description, StepStatus.COMPLETED,
                LocalDateTime.now(), compensationData, null);
    }
    
    public SagaStep fail(String errorMessage) {
        return new SagaStep(this.stepId, this.description, StepStatus.FAILED,
                LocalDateTime.now(), null, errorMessage);
    }
    
    public SagaStep compensate() {
        return new SagaStep(this.stepId, this.description, StepStatus.COMPENSATED,
                LocalDateTime.now(), this.compensationData, null);
    }
    
    public boolean isCompensatable() {
        return status == StepStatus.COMPLETED && compensationData != null;
    }
    
    public String getStepId() {
        return stepId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public StepStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
    
    public String getCompensationData() {
        return compensationData;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}