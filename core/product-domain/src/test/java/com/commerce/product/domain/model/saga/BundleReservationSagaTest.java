package com.commerce.product.domain.model.saga;

import com.commerce.product.domain.model.SkuMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BundleReservationSaga 테스트")
class BundleReservationSagaTest {
    
    @Test
    @DisplayName("Saga를 생성하면 STARTED 상태여야 한다")
    void shouldCreateSagaWithStartedStatus() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 2, "SKU002", 1));
        
        // When
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 3);
        
        // Then
        assertNotNull(saga.getSagaId());
        assertEquals(SagaStatus.STARTED, saga.getStatus());
        assertEquals("RES-001", saga.getReservationId());
        assertEquals("ORDER-001", saga.getOrderId());
        assertEquals(3, saga.getQuantity());
        assertEquals(2, saga.getSteps().size());
        assertNotNull(saga.getStartedAt());
        assertNull(saga.getCompletedAt());
    }
    
    @Test
    @DisplayName("Saga를 시작하면 IN_PROGRESS 상태가 되어야 한다")
    void shouldStartSaga() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1));
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 1);
        
        // When
        saga.start();
        
        // Then
        assertEquals(SagaStatus.IN_PROGRESS, saga.getStatus());
    }
    
    @Test
    @DisplayName("시작되지 않은 상태에서만 시작할 수 있다")
    void shouldOnlyStartFromStartedStatus() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1));
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 1);
        saga.start();
        
        // When & Then
        assertThrows(IllegalStateException.class, saga::start);
    }
    
    @Test
    @DisplayName("스텝 성공을 기록할 수 있다")
    void shouldRecordStepSuccess() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1));
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 1);
        saga.start();
        
        // When
        saga.recordStepSuccess("SKU001", "RESERVATION-123");
        
        // Then
        SagaStep step = saga.getSteps().get(0);
        assertEquals(SagaStep.StepStatus.COMPLETED, step.getStatus());
        assertEquals("RESERVATION-123", step.getCompensationData());
        assertEquals(SagaStatus.COMPLETED, saga.getStatus()); // 모든 스텝이 완료됨
    }
    
    @Test
    @DisplayName("스텝 실패를 기록하면 보상이 시작되어야 한다")
    void shouldStartCompensationOnStepFailure() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1, "SKU002", 1));
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 1);
        saga.start();
        saga.recordStepSuccess("SKU001", "RESERVATION-123");
        
        // When
        saga.recordStepFailure("SKU002", "Insufficient stock");
        
        // Then
        assertEquals(SagaStatus.COMPENSATING, saga.getStatus());
        assertEquals("Insufficient stock", saga.getFailureReason());
    }
    
    @Test
    @DisplayName("보상 가능한 스텝들을 역순으로 반환해야 한다")
    void shouldReturnCompensatableStepsInReverse() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1, "SKU002", 1, "SKU003", 1));
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 1);
        saga.start();
        saga.recordStepSuccess("SKU001", "RESERVATION-001");
        saga.recordStepSuccess("SKU002", "RESERVATION-002");
        saga.recordStepFailure("SKU003", "Failed");
        
        // When
        var compensatableSteps = saga.getCompensatableSteps();
        
        // Then
        assertEquals(2, compensatableSteps.size());
        assertEquals("SKU002", compensatableSteps.get(0).getStepId());
        assertEquals("SKU001", compensatableSteps.get(1).getStepId());
    }
    
    @Test
    @DisplayName("보상을 기록할 수 있다")
    void shouldRecordCompensation() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1, "SKU002", 1));
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 1);
        saga.start();
        saga.recordStepSuccess("SKU001", "RESERVATION-001");
        saga.recordStepFailure("SKU002", "Failed");
        
        // When
        saga.recordCompensation("SKU001");
        
        // Then
        SagaStep step = saga.getSteps().stream()
                .filter(s -> s.getStepId().equals("SKU001"))
                .findFirst()
                .orElseThrow();
        assertEquals(SagaStep.StepStatus.COMPENSATED, step.getStatus());
        assertEquals(SagaStatus.COMPENSATED, saga.getStatus()); // 모든 보상 완료
    }
    
    @Test
    @DisplayName("터미널 상태를 확인할 수 있다")
    void shouldCheckTerminalStatus() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1));
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 1);
        
        // When & Then
        assertFalse(saga.isTerminal());
        
        saga.start();
        assertFalse(saga.isTerminal());
        
        saga.recordStepSuccess("SKU001", "RESERVATION-001");
        assertTrue(saga.isTerminal()); // COMPLETED
    }
    
    @Test
    @DisplayName("실패 이유와 함께 실패시킬 수 있다")
    void shouldFailWithReason() {
        // Given
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1));
        BundleReservationSaga saga = new BundleReservationSaga("RES-001", "ORDER-001", skuMapping, 1);
        
        // When
        saga.fail("Unexpected error occurred");
        
        // Then
        assertEquals(SagaStatus.FAILED, saga.getStatus());
        assertEquals("Unexpected error occurred", saga.getFailureReason());
        assertNotNull(saga.getCompletedAt());
        assertTrue(saga.isTerminal());
    }
}