package com.commerce.inventory.domain.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidReservationException;
import com.commerce.inventory.domain.exception.InvalidSkuIdException;
import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.domain.repository.InventoryRepository;
import com.commerce.inventory.domain.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReserveStockUseCaseTest {
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private Clock clock;
    
    private ReserveStockUseCase useCase;
    
    private LocalDateTime fixedTime;
    
    @BeforeEach
    void setUp() {
        useCase = new ReserveStockUseCase(inventoryRepository, reservationRepository, clock);
        fixedTime = LocalDateTime.of(2024, 1, 1, 10, 0);
    }
    
    private void setupClock() {
        when(clock.instant()).thenReturn(fixedTime.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }
    
    @Test
    @DisplayName("단일 SKU 재고 예약에 성공한다")
    void reserveSingleSku() {
        // Given
        setupClock();
        SkuId skuId = new SkuId("SKU-001");
        Inventory inventory = createInventoryWithStock(skuId, 100, 10);
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(skuId, inventory);
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(inventoryMap);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-001")
                                .quantity(5)
                                .build()
                ))
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When
        ReserveStockResponse response = useCase.execute(request);
        
        // Then
        assertThat(response.getReservations()).hasSize(1);
        
        ReserveStockResponse.ReservationResult result = response.getReservations().get(0);
        assertThat(result.getSkuId()).isEqualTo("SKU-001");
        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getExpiresAt()).isEqualTo(fixedTime.plusSeconds(900));
        
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        Inventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory).isEqualTo(inventory);
        assertThat(savedInventory.getAvailableQuantity().value()).isEqualTo(85);
    }
    
    @Test
    @DisplayName("복수 SKU 재고 예약에 성공한다")
    void reserveMultipleSkus() {
        // Given
        setupClock();
        SkuId skuId1 = new SkuId("SKU-001");
        SkuId skuId2 = new SkuId("SKU-002");
        Inventory inventory1 = createInventoryWithStock(skuId1, 100, 10);
        Inventory inventory2 = createInventoryWithStock(skuId2, 50, 5);
        
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(skuId1, inventory1);
        inventoryMap.put(skuId2, inventory2);
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(inventoryMap);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-001")
                                .quantity(5)
                                .build(),
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-002")
                                .quantity(3)
                                .build()
                ))
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When
        ReserveStockResponse response = useCase.execute(request);
        
        // Then
        assertThat(response.getReservations()).hasSize(2);
        
        // 모든 재고가 한 번씩만 저장되는지 확인
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(2)).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getAllValues()).containsExactlyInAnyOrder(inventory1, inventory2);
        
        verify(reservationRepository, times(2)).save(any(Reservation.class));
    }
    
    @Test
    @DisplayName("재고가 부족하면 예외가 발생한다")
    void failWhenInsufficientStock() {
        // Given
        setupClock();
        SkuId skuId = new SkuId("SKU-001");
        Inventory inventory = createInventoryWithStock(skuId, 10, 5);
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(skuId, inventory);
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(inventoryMap);
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-001")
                                .quantity(10)
                                .build()
                ))
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고가 부족합니다");
        
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("존재하지 않는 SKU로 예약 시도 시 예외가 발생한다")
    void failWhenSkuNotFound() {
        // Given
        setupClock();
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(new HashMap<>());
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-999")
                                .quantity(5)
                                .build()
                ))
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidSkuIdException.class)
                .hasMessageContaining("SKU를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("예약 항목이 null일 때 예외가 발생한다")
    void failWhenItemsAreNull() {
        // Given
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(null)
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("예약 항목이 비어있습니다");
    }
    
    @Test
    @DisplayName("예약 항목이 비어있을 때 예외가 발생한다")
    void failWhenItemsAreEmpty() {
        // Given
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList())
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("예약 항목이 비어있습니다");
    }
    
    @Test
    @DisplayName("주문 ID가 null일 때 예외가 발생한다")
    void failWhenOrderIdIsNull() {
        // Given
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-001")
                                .quantity(5)
                                .build()
                ))
                .orderId(null)
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("주문 ID는 필수입니다");
    }
    
    @Test
    @DisplayName("TTL이 없으면 기본값을 사용한다")
    void useDefaultTtlWhenNotProvided() {
        // Given
        setupClock();
        SkuId skuId = new SkuId("SKU-001");
        Inventory inventory = createInventoryWithStock(skuId, 100, 10);
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(skuId, inventory);
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(inventoryMap);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-001")
                                .quantity(5)
                                .build()
                ))
                .orderId("ORDER-001")
                .ttlSeconds(null)
                .build();
        
        // When
        ReserveStockResponse response = useCase.execute(request);
        
        // Then
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        
        Reservation savedReservation = captor.getValue();
        assertThat(savedReservation.getExpiresAt()).isEqualTo(fixedTime.plusSeconds(900)); // 기본값 900초
    }
    
    @Test
    @DisplayName("예약이 일부만 성공하면 모두 롤백된다")
    void rollbackWhenPartialFailure() {
        // Given
        setupClock();
        SkuId skuId1 = new SkuId("SKU-001");
        SkuId skuId2 = new SkuId("SKU-002");
        Inventory inventory1 = createInventoryWithStock(skuId1, 100, 10);
        Inventory inventory2 = createInventoryWithStock(skuId2, 5, 3); // 부족한 재고
        
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(skuId1, inventory1);
        inventoryMap.put(skuId2, inventory2);
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(inventoryMap);
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-001")
                                .quantity(5)
                                .build(),
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-002")
                                .quantity(5) // 가용 재고보다 많음
                                .build()
                ))
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InsufficientStockException.class);
        
        // 첫 번째 예약도 저장되지 않아야 함
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }
    
    private Inventory createInventoryWithStock(SkuId skuId, int totalQuantity, int reservedQuantity) {
        return Inventory.create(
                skuId,
                Quantity.of(totalQuantity),
                Quantity.of(reservedQuantity)
        );
    }
    
    @Test
    @DisplayName("중복 SKU 요청의 총량이 재고를 초과하면 예외가 발생한다")
    void failWhenTotalQuantityOfDuplicateSkusExceedsStock() {
        // Given
        setupClock();
        SkuId skuId = new SkuId("SKU-001");
        Inventory inventory = createInventoryWithStock(skuId, 10, 2); // 가용 재고: 8
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(skuId, inventory);
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(inventoryMap);

        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder().skuId("SKU-001").quantity(5).build(),
                        ReserveStockRequest.ReservationItem.builder().skuId("SKU-001").quantity(5).build()
                ))
                .orderId("ORDER-001")
                .build();

        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고가 부족합니다")
                .hasMessageContaining("가용 재고: 8")
                .hasMessageContaining("요청 수량: 10");
        
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }
}