package com.commerce.product.domain.service.saga;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.event.BundleReservationCompletedEvent;
import com.commerce.product.domain.event.BundleReservationFailedEvent;
import com.commerce.product.domain.exception.LockAcquisitionException;
import com.commerce.product.domain.model.DistributedLock;
import com.commerce.product.domain.model.SkuMapping;
import com.commerce.product.domain.model.saga.BundleReservationSaga;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.SagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BundleReservationSagaOrchestrator 테스트")
class BundleReservationSagaOrchestratorTest {
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private LockRepository lockRepository;
    
    @Mock
    private SagaRepository sagaRepository;
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    @Mock
    private DistributedLock lock;
    
    private BundleReservationSagaOrchestrator orchestrator;
    
    @BeforeEach
    void setUp() {
        orchestrator = new BundleReservationSagaOrchestrator(
            inventoryRepository, lockRepository, sagaRepository, eventPublisher
        );
    }
    
    @Test
    @DisplayName("번들 재고 예약을 성공적으로 수행해야 한다")
    void shouldSuccessfullyReserveBundleStock() throws Exception {
        // Given
        String orderId = "ORDER-001";
        String reservationId = "RES-001";
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 2, "SKU002", 1));
        int quantity = 3;
        
        when(lockRepository.acquireLock(anyString(), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock));
        when(inventoryRepository.getAvailableQuantity("SKU001")).thenReturn(10);
        when(inventoryRepository.getAvailableQuantity("SKU002")).thenReturn(5);
        when(inventoryRepository.reserveStock("SKU001", 6, orderId)).thenReturn("RES-SKU001");
        when(inventoryRepository.reserveStock("SKU002", 3, orderId)).thenReturn("RES-SKU002");
        
        // When
        CompletableFuture<Void> future = orchestrator.execute(orderId, skuMapping, quantity, reservationId);
        future.join();
        
        // Then
        verify(sagaRepository).save(any(BundleReservationSaga.class));
        verify(sagaRepository, atLeastOnce()).update(any(BundleReservationSaga.class));
        verify(inventoryRepository).reserveStock("SKU001", 6, orderId);
        verify(inventoryRepository).reserveStock("SKU002", 3, orderId);
        verify(lockRepository, times(2)).releaseLock(lock);
        
        ArgumentCaptor<BundleReservationCompletedEvent> eventCaptor = 
                ArgumentCaptor.forClass(BundleReservationCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        BundleReservationCompletedEvent event = eventCaptor.getValue();
        assertEquals(reservationId, event.getReservationId());
        assertEquals(orderId, event.getOrderId());
    }
    
    @Test
    @DisplayName("재고 부족 시 보상 트랜잭션을 실행해야 한다")
    void shouldCompensateOnInsufficientStock() throws Exception {
        // Given
        String orderId = "ORDER-001";
        String reservationId = "RES-001";
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 2, "SKU002", 1));
        int quantity = 3;
        
        when(lockRepository.acquireLock(anyString(), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock));
        when(inventoryRepository.getAvailableQuantity("SKU001")).thenReturn(10);
        when(inventoryRepository.getAvailableQuantity("SKU002")).thenReturn(1); // 부족
        when(inventoryRepository.reserveStock("SKU001", 6, orderId)).thenReturn("RES-SKU001");
        
        // When
        CompletableFuture<Void> future = orchestrator.execute(orderId, skuMapping, quantity, reservationId);
        future.join();
        
        // Then
        verify(inventoryRepository).releaseReservation("RES-SKU001"); // 보상
        verify(eventPublisher).publish(any(BundleReservationFailedEvent.class));
    }
    
    @Test
    @DisplayName("락 획득 실패 시 예외를 던져야 한다")
    void shouldThrowExceptionOnLockAcquisitionFailure() {
        // Given
        String orderId = "ORDER-001";
        String reservationId = "RES-001";
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1));
        
        when(lockRepository.acquireLock(anyString(), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.empty());
        
        // When & Then
        CompletableFuture<Void> future = orchestrator.execute(orderId, skuMapping, 1, reservationId);
        
        assertThrows(CompletionException.class, future::join);
        verify(eventPublisher).publish(any(BundleReservationFailedEvent.class));
    }
    
    @Test
    @DisplayName("예약 중 예외 발생 시 보상을 실행해야 한다")
    void shouldCompensateOnReservationException() throws Exception {
        // Given
        String orderId = "ORDER-001";
        String reservationId = "RES-001";
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1, "SKU002", 1));
        
        when(lockRepository.acquireLock(anyString(), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock));
        when(inventoryRepository.getAvailableQuantity("SKU001")).thenReturn(10);
        when(inventoryRepository.getAvailableQuantity("SKU002")).thenReturn(10);
        when(inventoryRepository.reserveStock("SKU001", 1, orderId)).thenReturn("RES-SKU001");
        when(inventoryRepository.reserveStock("SKU002", 1, orderId))
                .thenThrow(new RuntimeException("Database error"));
        
        // When
        CompletableFuture<Void> future = orchestrator.execute(orderId, skuMapping, 1, reservationId);
        future.join();
        
        // Then
        verify(inventoryRepository).releaseReservation("RES-SKU001"); // 보상
        verify(eventPublisher).publish(any(BundleReservationFailedEvent.class));
    }
    
    @Test
    @DisplayName("보상 중 예외가 발생해도 계속 진행해야 한다")
    void shouldContinueCompensationDespiteException() throws Exception {
        // Given
        String orderId = "ORDER-001";
        String reservationId = "RES-001";
        SkuMapping skuMapping = SkuMapping.of(Map.of("SKU001", 1, "SKU002", 1, "SKU003", 1));
        
        when(lockRepository.acquireLock(anyString(), any(Duration.class), any(Duration.class)))
                .thenReturn(Optional.of(lock));
        when(inventoryRepository.getAvailableQuantity(anyString())).thenReturn(10);
        when(inventoryRepository.reserveStock("SKU001", 1, orderId)).thenReturn("RES-SKU001");
        when(inventoryRepository.reserveStock("SKU002", 1, orderId)).thenReturn("RES-SKU002");
        when(inventoryRepository.reserveStock("SKU003", 1, orderId))
                .thenThrow(new RuntimeException("Failed"));
        
        doThrow(new RuntimeException("Compensation failed"))
                .when(inventoryRepository).releaseReservation("RES-SKU001");
        
        // When
        CompletableFuture<Void> future = orchestrator.execute(orderId, skuMapping, 1, reservationId);
        future.join();
        
        // Then
        verify(inventoryRepository).releaseReservation("RES-SKU002"); // SKU001 실패해도 SKU002는 보상
        verify(eventPublisher).publish(any(BundleReservationFailedEvent.class));
    }
}