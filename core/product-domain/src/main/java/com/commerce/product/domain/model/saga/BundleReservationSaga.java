package com.commerce.product.domain.model.saga;

import com.commerce.product.domain.model.AggregateRoot;
import com.commerce.product.domain.model.SkuMapping;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class BundleReservationSaga extends AggregateRoot<SagaId> {
    
    private final SagaId sagaId;
    private final String reservationId;
    private final String orderId;
    private final SkuMapping skuMapping;
    private final int quantity;
    private SagaStatus status;
    private final LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private final List<SagaStep> steps;
    private String failureReason;
    
    public BundleReservationSaga(String reservationId, String orderId, SkuMapping skuMapping, int quantity) {
        this.sagaId = SagaId.generate();
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.skuMapping = skuMapping;
        this.quantity = quantity;
        this.status = SagaStatus.STARTED;
        this.startedAt = LocalDateTime.now();
        this.steps = new ArrayList<>();
        
        // 각 SKU에 대한 예약 스텝 생성
        for (Map.Entry<String, Integer> entry : skuMapping.mappings().entrySet()) {
            String skuId = entry.getKey();
            int requiredQuantity = entry.getValue() * quantity;
            steps.add(SagaStep.pending(
                skuId, 
                String.format("Reserve %d units of SKU %s", requiredQuantity, skuId)
            ));
        }
    }
    
    public void start() {
        if (status != SagaStatus.STARTED) {
            throw new IllegalStateException("Saga can only be started from STARTED status");
        }
        this.status = SagaStatus.IN_PROGRESS;
    }
    
    public void recordStepSuccess(String stepId, String compensationData) {
        SagaStep step = findStep(stepId);
        int index = steps.indexOf(step);
        steps.set(index, step.complete(compensationData));
        
        // 모든 스텝이 완료되었는지 확인
        if (steps.stream().allMatch(s -> s.getStatus() == SagaStep.StepStatus.COMPLETED)) {
            complete();
        }
    }
    
    public void recordStepFailure(String stepId, String errorMessage) {
        SagaStep step = findStep(stepId);
        int index = steps.indexOf(step);
        steps.set(index, step.fail(errorMessage));
        this.failureReason = errorMessage;
        startCompensation();
    }
    
    private void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    private void startCompensation() {
        if (!status.canCompensate()) {
            throw new IllegalStateException("Cannot start compensation from status: " + status);
        }
        this.status = SagaStatus.COMPENSATING;
    }
    
    public void recordCompensation(String stepId) {
        SagaStep step = findStep(stepId);
        if (!step.isCompensatable()) {
            return; // 보상할 수 없는 스텝은 건너뜀
        }
        
        int index = steps.indexOf(step);
        steps.set(index, step.compensate());
        
        // 모든 완료된 스텝이 보상되었는지 확인
        boolean allCompensated = steps.stream()
                .filter(SagaStep::isCompensatable)
                .allMatch(s -> s.getStatus() == SagaStep.StepStatus.COMPENSATED);
                
        if (allCompensated) {
            this.status = SagaStatus.COMPENSATED;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public void fail(String reason) {
        this.status = SagaStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
    }
    
    public List<SagaStep> getCompensatableSteps() {
        List<SagaStep> compensatableSteps = new ArrayList<>();
        for (SagaStep step : steps) {
            if (step.isCompensatable()) {
                compensatableSteps.add(step);
            }
        }
        // 역순으로 보상 실행
        Collections.reverse(compensatableSteps);
        return compensatableSteps;
    }
    
    private SagaStep findStep(String stepId) {
        return steps.stream()
                .filter(step -> step.getStepId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
    }
    
    public boolean isTerminal() {
        return status.isTerminal();
    }
    
    @Override
    public SagaId getId() {
        return sagaId;
    }
}