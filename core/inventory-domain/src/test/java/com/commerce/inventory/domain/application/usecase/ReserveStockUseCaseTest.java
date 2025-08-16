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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    
    @Captor
    private ArgumentCaptor<List<Inventory>> inventoryCaptor;
    
    @Captor
    private ArgumentCaptor<List<Reservation>> reservationCaptor;
    
    @Captor
    private ArgumentCaptor<Set<SkuId>> skuIdCaptor;
    
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
        when(reservationRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
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
        
        verify(inventoryRepository).saveAll(inventoryCaptor.capture());
        List<Inventory> savedInventories = inventoryCaptor.getValue();
        assertThat(savedInventories).hasSize(1);
        assertThat(savedInventories.get(0)).isEqualTo(inventory);
        assertThat(savedInventories.get(0).getAvailableQuantity().value()).isEqualTo(85);
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
        when(reservationRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
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
        
        // 모든 재고가 일괄 저장되는지 확인
        verify(inventoryRepository).saveAll(inventoryCaptor.capture());
        List<Inventory> savedInventories = inventoryCaptor.getValue();
        assertThat(savedInventories).hasSize(2);
        assertThat(savedInventories).containsExactlyInAnyOrder(inventory1, inventory2);
        
        verify(reservationRepository).saveAll(reservationCaptor.capture());
        List<Reservation> savedReservations = reservationCaptor.getValue();
        assertThat(savedReservations).hasSize(2);
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
        
        verify(inventoryRepository, never()).saveAll(any());
        verify(reservationRepository, never()).saveAll(any());
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
                .hasMessageContaining("다음 SKU를 찾을 수 없습니다: SKU-999");
    }
    
    @Test
    @DisplayName("여러 개의 존재하지 않는 SKU로 예약 시도 시 모든 SKU를 보고한다")
    void failWhenMultipleSkusNotFound() {
        // Given
        setupClock();
        SkuId skuId1 = new SkuId("SKU-001");
        Inventory inventory1 = createInventoryWithStock(skuId1, 100, 10);
        Map<SkuId, Inventory> inventoryMap = new HashMap<>();
        inventoryMap.put(skuId1, inventory1);
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(inventoryMap);
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(Arrays.asList(
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-001")
                                .quantity(5)
                                .build(),
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-999")
                                .quantity(3)
                                .build(),
                        ReserveStockRequest.ReservationItem.builder()
                                .skuId("SKU-888")
                                .quantity(2)
                                .build()
                ))
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidSkuIdException.class)
                .hasMessageContaining("다음 SKU를 찾을 수 없습니다")
                .hasMessageContaining("SKU-999")
                .hasMessageContaining("SKU-888");
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
                .hasMessageContaining("예약 항목이(가) 비어있습니다");
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
                .hasMessageContaining("예약 항목이(가) 비어있습니다");
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
                .hasMessageContaining("주문 ID은(는) 필수입니다");
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
        when(reservationRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
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
        verify(reservationRepository).saveAll(reservationCaptor.capture());
        
        List<Reservation> savedReservations = reservationCaptor.getValue();
        assertThat(savedReservations).hasSize(1);
        assertThat(savedReservations.get(0).getExpiresAt()).isEqualTo(fixedTime.plusSeconds(900)); // 기본값 900초
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
        verify(inventoryRepository, never()).saveAll(any());
        verify(reservationRepository, never()).saveAll(any());
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
        
        verify(inventoryRepository, never()).saveAll(any());
        verify(reservationRepository, never()).saveAll(any());
    }
    
    @Test
    @DisplayName("예약 항목 중 null이 포함되어 있으면 예외가 발생한다")
    void failWhenItemIsNull() {
        // Given
        List<ReserveStockRequest.ReservationItem> items = new ArrayList<>();
        items.add(ReserveStockRequest.ReservationItem.builder()
                .skuId("SKU-001")
                .quantity(5)
                .build());
        items.add(null); // null 항목 추가
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(items)
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("예약 항목에 null이 포함될 수 없습니다");
    }
    
    @Test
    @DisplayName("존재하지 않는 SKU가 10개를 초과하면 메시지가 제한된다")
    void shouldLimitMissingSkuIdsInExceptionMessage() {
        // Given
        setupClock();
        
        // 15개의 존재하지 않는 SKU 준비
        List<ReserveStockRequest.ReservationItem> items = IntStream.range(1, 16)
                .mapToObj(i -> ReserveStockRequest.ReservationItem.builder()
                        .skuId("MISSING-SKU-" + i)
                        .quantity(1)
                        .build())
                .collect(Collectors.toList());
        
        // 빈 Map 반환하여 모든 SKU가 존재하지 않도록 설정
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenReturn(new HashMap<>());
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(items)
                .orderId("ORDER-001")
                .ttlSeconds(900)
                .build();
        
        // When/Then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidSkuIdException.class)
                .hasMessageContaining("다음 SKU를 찾을 수 없습니다:")
                .hasMessageContaining("외 5개")
                .satisfies(throwable -> {
                    String message = throwable.getMessage();
                    // 메시지에 SKU가 10개만 나열되는지 확인
                    int skuCount = message.split("MISSING-SKU-").length - 1;
                    assertThat(skuCount).isEqualTo(10);
                });
    }
    
    @Test
    @DisplayName("대량의 SKU(1000개 초과)를 배치로 나누어 조회한다")
    void reserveLargeBatchOfSkus() {
        // Given
        setupClock();
        int totalSkus = 2500; // BATCH_SIZE(1000)를 초과하는 수량
        
        // 2500개의 SKU와 재고를 생성
        Map<SkuId, Inventory> allInventories = new HashMap<>();
        List<ReserveStockRequest.ReservationItem> items = IntStream.range(1, totalSkus + 1)
                .mapToObj(i -> {
                    SkuId skuId = new SkuId("SKU-" + String.format("%04d", i));
                    Inventory inventory = createInventoryWithStock(skuId, 100, 10);
                    allInventories.put(skuId, inventory);
                    return ReserveStockRequest.ReservationItem.builder()
                            .skuId(skuId.value())
                            .quantity(1)
                            .build();
                })
                .collect(Collectors.toList());
        
        // Mock 설정: 어떤 Set이 들어와도 해당 SKU에 대한 재고만 반환
        when(inventoryRepository.findBySkuIdsWithLock(any(Set.class))).thenAnswer(invocation -> {
            Set<SkuId> requestedSkuIds = invocation.getArgument(0);
            return requestedSkuIds.stream()
                    .filter(allInventories::containsKey)
                    .collect(Collectors.toMap(
                            skuId -> skuId,
                            allInventories::get
                    ));
        });
        
        when(reservationRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        ReserveStockRequest request = ReserveStockRequest.builder()
                .items(items)
                .orderId("ORDER-LARGE")
                .ttlSeconds(900)
                .build();
        
        // When
        ReserveStockResponse response = useCase.execute(request);
        
        // Then
        assertThat(response.getReservations()).hasSize(totalSkus);
        
        // findBySkuIdsWithLock이 3번 호출되었는지 확인 (2500 / 1000 = 3 배치)
        verify(inventoryRepository, times(3)).findBySkuIdsWithLock(any(Set.class));
        
        // 각 배치 호출의 크기 검증
        verify(inventoryRepository, times(3)).findBySkuIdsWithLock(skuIdCaptor.capture());
        
        List<Set<SkuId>> capturedSets = skuIdCaptor.getAllValues();
        assertThat(capturedSets.get(0)).hasSize(1000); // 첫 번째 배치
        assertThat(capturedSets.get(1)).hasSize(1000); // 두 번째 배치
        assertThat(capturedSets.get(2)).hasSize(500);  // 마지막 배치
        
        // 모든 재고가 저장되었는지 확인
        verify(inventoryRepository).saveAll(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue()).hasSize(totalSkus);
    }
}