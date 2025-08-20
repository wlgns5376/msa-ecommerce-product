package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.BundleReservationResponse;
import com.commerce.inventory.application.port.in.BundleReservationStatus;
import com.commerce.inventory.application.port.in.ReserveBundleStockCommand;
import com.commerce.inventory.application.port.in.SkuReservationStatus;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.exception.InsufficientStockException;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.domain.repository.ReservationRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReserveBundleStockService 테스트")
class ReserveBundleStockServiceTest {

    @Mock
    private LoadInventoryPort loadInventoryPort;

    @Mock
    private SaveInventoryPort saveInventoryPort;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private Validator validator;

    private Clock fixedClock;
    private ReserveBundleStockService sut;

    @BeforeEach
    void setUp() {
        // 고정된 시간으로 Clock 생성
        Instant fixedInstant = Instant.parse("2024-01-01T10:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        
        sut = new ReserveBundleStockService(
            loadInventoryPort,
            saveInventoryPort,
            reservationRepository,
            fixedClock,
            validator
        );
        
        // 기본적으로 validation은 통과하도록 설정
        given(validator.validate(any())).willReturn(Collections.emptySet());
    }

    @Test
    @DisplayName("단일 번들 상품의 재고를 성공적으로 예약한다")
    void reserveBundleStock_singleBundle_success() {
        // Given
        String orderId = "ORDER-001";
        String reservationId = "BUNDLE-RESERVATION-001";
        
        ReserveBundleStockCommand.BundleItem bundleItem = ReserveBundleStockCommand.BundleItem.builder()
            .productOptionId("OPTION-001")
            .skuMappings(List.of(
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-001")
                    .quantity(2)
                    .build(),
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-002")
                    .quantity(1)
                    .build()
            ))
            .quantity(1)
            .build();

        ReserveBundleStockCommand command = ReserveBundleStockCommand.builder()
            .orderId(orderId)
            .reservationId(reservationId)
            .bundleItems(List.of(bundleItem))
            .ttlSeconds(900)
            .build();

        // 재고 설정
        Inventory inventory1 = Inventory.createWithInitialStock(
            new SkuId("SKU-001"), 
            Quantity.of(10)
        );
        Inventory inventory2 = Inventory.createWithInitialStock(
            new SkuId("SKU-002"), 
            Quantity.of(5)
        );

        Map<SkuId, Inventory> inventoryMap = Map.of(
            new SkuId("SKU-001"), inventory1,
            new SkuId("SKU-002"), inventory2
        );
        given(loadInventoryPort.loadAllByIds(anyList())).willReturn(inventoryMap);
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        BundleReservationResponse response = sut.execute(command);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSagaId()).isEqualTo(reservationId);
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo(BundleReservationStatus.COMPLETED);
        assertThat(response.getSkuReservations()).hasSize(2);
        assertThat(response.getFailureReason()).isNull();

        // 각 SKU 예약 확인
        BundleReservationResponse.SkuReservation skuReservation1 = 
            response.getSkuReservations().stream()
                .filter(r -> r.getSkuId().equals("SKU-001"))
                .findFirst()
                .orElseThrow();
        
        assertThat(skuReservation1.getQuantity()).isEqualTo(2);
        assertThat(skuReservation1.getStatus()).isEqualTo(SkuReservationStatus.ACTIVE);
        assertThat(skuReservation1.getExpiresAt()).isAfter(LocalDateTime.now(fixedClock));

        // 재고 저장 확인
        then(saveInventoryPort).should(times(1)).saveAll(anyCollection());
        then(reservationRepository).should(times(2)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("번들 상품의 일부 SKU 재고가 부족하면 전체 예약이 실패한다")
    void reserveBundleStock_insufficientStock_fail() {
        // Given
        ReserveBundleStockCommand.BundleItem bundleItem = ReserveBundleStockCommand.BundleItem.builder()
            .productOptionId("OPTION-001")
            .skuMappings(List.of(
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-001")
                    .quantity(2)
                    .build(),
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-002")
                    .quantity(3)
                    .build()
            ))
            .quantity(2) // 2세트 주문
            .build();

        ReserveBundleStockCommand command = ReserveBundleStockCommand.builder()
            .orderId("ORDER-001")
            .reservationId("BUNDLE-RESERVATION-001")
            .bundleItems(List.of(bundleItem))
            .ttlSeconds(900)
            .build();

        // SKU-001: 충분한 재고, SKU-002: 부족한 재고
        Inventory inventory1 = Inventory.createWithInitialStock(
            new SkuId("SKU-001"), 
            Quantity.of(10)
        );
        Inventory inventory2 = Inventory.createWithInitialStock(
            new SkuId("SKU-002"), 
            Quantity.of(5) // 6개 필요하지만 5개만 있음
        );

        Map<SkuId, Inventory> inventoryMap = Map.of(
            new SkuId("SKU-001"), inventory1,
            new SkuId("SKU-002"), inventory2
        );
        given(loadInventoryPort.loadAllByIds(anyList())).willReturn(inventoryMap);
        // 리팩토링 후 사전 검증에서 재고 부족이 감지되므로 reservation save 모킹 불필요

        // When
        BundleReservationResponse response = sut.execute(command);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BundleReservationStatus.FAILED);
        assertThat(response.getFailureReason()).contains("재고가 부족합니다");
        assertThat(response.getSkuReservations()).isEmpty();
    }

    @Test
    @DisplayName("여러 번들 상품을 동시에 예약할 수 있다")
    void reserveBundleStock_multipleBundles_success() {
        // Given
        ReserveBundleStockCommand.BundleItem bundleItem1 = ReserveBundleStockCommand.BundleItem.builder()
            .productOptionId("OPTION-001")
            .skuMappings(List.of(
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-001")
                    .quantity(1)
                    .build()
            ))
            .quantity(2)
            .build();

        ReserveBundleStockCommand.BundleItem bundleItem2 = ReserveBundleStockCommand.BundleItem.builder()
            .productOptionId("OPTION-002")
            .skuMappings(List.of(
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-002")
                    .quantity(1)
                    .build(),
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-003")
                    .quantity(2)
                    .build()
            ))
            .quantity(1)
            .build();

        ReserveBundleStockCommand command = ReserveBundleStockCommand.builder()
            .orderId("ORDER-001")
            .reservationId("BUNDLE-RESERVATION-001")
            .bundleItems(List.of(bundleItem1, bundleItem2))
            .ttlSeconds(900)
            .build();

        // 재고 설정
        Inventory inventory1 = Inventory.createWithInitialStock(new SkuId("SKU-001"), Quantity.of(10));
        Inventory inventory2 = Inventory.createWithInitialStock(new SkuId("SKU-002"), Quantity.of(5));
        Inventory inventory3 = Inventory.createWithInitialStock(new SkuId("SKU-003"), Quantity.of(8));

        Map<SkuId, Inventory> inventoryMap = Map.of(
            new SkuId("SKU-001"), inventory1,
            new SkuId("SKU-002"), inventory2,
            new SkuId("SKU-003"), inventory3
        );
        given(loadInventoryPort.loadAllByIds(anyList())).willReturn(inventoryMap);
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        BundleReservationResponse response = sut.execute(command);

        // Then
        assertThat(response.getStatus()).isEqualTo(BundleReservationStatus.COMPLETED);
        assertThat(response.getSkuReservations()).hasSize(3);
        
        // SKU-001은 2개 예약되었는지 확인
        int sku001TotalQuantity = response.getSkuReservations().stream()
            .filter(r -> r.getSkuId().equals("SKU-001"))
            .mapToInt(BundleReservationResponse.SkuReservation::getQuantity)
            .sum();
        assertThat(sku001TotalQuantity).isEqualTo(2);

        then(saveInventoryPort).should(times(1)).saveAll(anyCollection());
        then(reservationRepository).should(times(3)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("SKU가 존재하지 않으면 예약이 실패한다")
    void reserveBundleStock_skuNotFound_fail() {
        // Given
        ReserveBundleStockCommand.BundleItem bundleItem = ReserveBundleStockCommand.BundleItem.builder()
            .productOptionId("OPTION-001")
            .skuMappings(List.of(
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-001")
                    .quantity(1)
                    .build(),
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-NOT-EXIST")
                    .quantity(1)
                    .build()
            ))
            .quantity(1)
            .build();

        ReserveBundleStockCommand command = ReserveBundleStockCommand.builder()
            .orderId("ORDER-001")
            .reservationId("BUNDLE-RESERVATION-001")
            .bundleItems(List.of(bundleItem))
            .ttlSeconds(900)
            .build();

        Inventory inventory1 = Inventory.createWithInitialStock(new SkuId("SKU-001"), Quantity.of(10));
        
        Map<SkuId, Inventory> inventoryMap = Map.of(
            new SkuId("SKU-001"), inventory1
        );
        given(loadInventoryPort.loadAllByIds(anyList())).willReturn(inventoryMap);

        // When
        BundleReservationResponse response = sut.execute(command);

        // Then
        assertThat(response.getStatus()).isEqualTo(BundleReservationStatus.FAILED);
        assertThat(response.getFailureReason()).contains("다음 SKU에 대한 재고 정보를 찾을 수 없습니다");
        assertThat(response.getFailureReason()).contains("SKU-NOT-EXIST");
        assertThat(response.getSkuReservations()).isEmpty();
        
        // 사전 검증에서 실패하므로 save 메서드는 호출되지 않음
        then(saveInventoryPort).should(never()).saveAll(anyCollection());
        then(reservationRepository).should(never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("예약 중 일부가 실패하면 트랜잭션이 롤백되어 전체 예약이 취소된다")
    void reserveBundleStock_partialFailure_transactionRollback() {
        // Given
        ReserveBundleStockCommand.BundleItem bundleItem = ReserveBundleStockCommand.BundleItem.builder()
            .productOptionId("OPTION-001")
            .skuMappings(List.of(
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-001")
                    .quantity(2)
                    .build(),
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-002")
                    .quantity(1)
                    .build(),
                ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-003")
                    .quantity(3)
                    .build()
            ))
            .quantity(1)
            .build();

        ReserveBundleStockCommand command = ReserveBundleStockCommand.builder()
            .orderId("ORDER-001")
            .reservationId("BUNDLE-RESERVATION-001")
            .bundleItems(List.of(bundleItem))
            .ttlSeconds(900)
            .build();

        // SKU-001, SKU-002는 충분한 재고, SKU-003는 부족한 재고
        Inventory inventory1 = Inventory.createWithInitialStock(new SkuId("SKU-001"), Quantity.of(10));
        Inventory inventory2 = Inventory.createWithInitialStock(new SkuId("SKU-002"), Quantity.of(5));
        Inventory inventory3 = Inventory.createWithInitialStock(new SkuId("SKU-003"), Quantity.of(2)); // 3개 필요하지만 2개만 있음

        // bulk 조회를 위한 mocking
        Map<SkuId, Inventory> inventoryMap = Map.of(
            new SkuId("SKU-001"), inventory1,
            new SkuId("SKU-002"), inventory2,
            new SkuId("SKU-003"), inventory3
        );
        given(loadInventoryPort.loadAllByIds(anyList())).willReturn(inventoryMap);
        
        // 리팩토링 후 사전 검증에서 재고 부족이 감지되므로 
        // reservation save 모킹이 더 이상 필요하지 않음

        // When
        BundleReservationResponse response = sut.execute(command);

        // Then
        assertThat(response.getStatus()).isEqualTo(BundleReservationStatus.FAILED);
        assertThat(response.getFailureReason()).contains("재고가 부족합니다");
        assertThat(response.getSkuReservations()).isEmpty();
        
        // 리팩토링으로 인해 사전 검증 단계에서 재고 부족이 감지되므로
        // 실제 예약 로직이 실행되지 않아 save 메서드들이 호출되지 않음
        then(saveInventoryPort).should(never()).saveAll(anyCollection());
        then(saveInventoryPort).should(never()).save(any(Inventory.class));
        then(reservationRepository).should(never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("TTL이 설정되지 않으면 기본값 900초를 사용한다")
    void reserveBundleStock_defaultTTL() {
        // Given
        ReserveBundleStockCommand command = ReserveBundleStockCommand.builder()
            .orderId("ORDER-001")
            .reservationId("BUNDLE-RESERVATION-001")
            .bundleItems(List.of(
                ReserveBundleStockCommand.BundleItem.builder()
                    .productOptionId("OPTION-001")
                    .skuMappings(List.of(
                        ReserveBundleStockCommand.SkuMapping.builder()
                            .skuId("SKU-001")
                            .quantity(1)
                            .build()
                    ))
                    .quantity(1)
                    .build()
            ))
            .ttlSeconds(null) // TTL 미설정
            .build();

        Inventory inventory = Inventory.createWithInitialStock(new SkuId("SKU-001"), Quantity.of(10));
        Map<SkuId, Inventory> inventoryMap = Map.of(
            new SkuId("SKU-001"), inventory
        );
        given(loadInventoryPort.loadAllByIds(anyList())).willReturn(inventoryMap);
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        BundleReservationResponse response = sut.execute(command);

        // Then
        assertThat(response.getStatus()).isEqualTo(BundleReservationStatus.COMPLETED);
        
        // 예약 만료 시간이 대략 900초 후인지 확인
        LocalDateTime expectedExpiry = LocalDateTime.now(fixedClock).plusSeconds(900);
        assertThat(response.getSkuReservations().get(0).getExpiresAt())
            .isBetween(expectedExpiry.minusSeconds(5), expectedExpiry.plusSeconds(5));
    }

    @Test
    @DisplayName("동일한 SKU가 여러 번들에 포함된 경우 각각 예약된다")
    void reserveBundleStock_sameSkuInMultipleBundles() {
        // Given
        ReserveBundleStockCommand command = ReserveBundleStockCommand.builder()
            .orderId("ORDER-001")
            .reservationId("BUNDLE-RESERVATION-001")
            .bundleItems(List.of(
                ReserveBundleStockCommand.BundleItem.builder()
                    .productOptionId("OPTION-001")
                    .skuMappings(List.of(
                        ReserveBundleStockCommand.SkuMapping.builder()
                            .skuId("SKU-001")
                            .quantity(2)
                            .build()
                    ))
                    .quantity(1)
                    .build(),
                ReserveBundleStockCommand.BundleItem.builder()
                    .productOptionId("OPTION-002")
                    .skuMappings(List.of(
                        ReserveBundleStockCommand.SkuMapping.builder()
                            .skuId("SKU-001") // 동일한 SKU
                            .quantity(3)
                            .build()
                    ))
                    .quantity(1)
                    .build()
            ))
            .ttlSeconds(900)
            .build();

        Inventory inventory = Inventory.createWithInitialStock(new SkuId("SKU-001"), Quantity.of(10));
        Map<SkuId, Inventory> inventoryMap = Map.of(
            new SkuId("SKU-001"), inventory
        );
        given(loadInventoryPort.loadAllByIds(anyList())).willReturn(inventoryMap);
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        BundleReservationResponse response = sut.execute(command);

        // Then
        assertThat(response.getStatus()).isEqualTo(BundleReservationStatus.COMPLETED);
        assertThat(response.getSkuReservations()).hasSize(2);
        
        // 총 5개가 예약되었는지 확인
        int totalReserved = response.getSkuReservations().stream()
            .mapToInt(BundleReservationResponse.SkuReservation::getQuantity)
            .sum();
        assertThat(totalReserved).isEqualTo(5);
        
        // 재고의 예약 수량 확인
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(5));
    }

    @Test
    @DisplayName("잘못된 입력값이 주어지면 예외가 발생한다")
    void reserveBundleStock_invalidInput_throwsException() {
        // Given - null command
        assertThatThrownBy(() -> sut.execute(null))
            .isInstanceOf(IllegalArgumentException.class);

        // Given - empty order ID
        ReserveBundleStockCommand emptyOrderIdCommand = ReserveBundleStockCommand.builder()
            .orderId("")
            .reservationId("BUNDLE-RESERVATION-001")
            .bundleItems(List.of(ReserveBundleStockCommand.BundleItem.builder()
                .productOptionId("OPTION-001")
                .skuMappings(List.of(ReserveBundleStockCommand.SkuMapping.builder()
                    .skuId("SKU-001")
                    .quantity(1)
                    .build()))
                .quantity(1)
                .build()))
            .build();
        
        // Mock validation error for empty order ID
        ConstraintViolation<ReserveBundleStockCommand> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("주문 ID는 필수입니다");
        when(validator.validate(emptyOrderIdCommand)).thenReturn(Set.of(violation));
        
        assertThatThrownBy(() -> sut.execute(emptyOrderIdCommand))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("주문 ID");

        // Given - empty bundle items
        ReserveBundleStockCommand emptyItemsCommand = ReserveBundleStockCommand.builder()
            .orderId("ORDER-001")
            .reservationId("BUNDLE-RESERVATION-001")
            .bundleItems(List.of())
            .build();
        
        // Mock validation error for empty bundle items
        ConstraintViolation<ReserveBundleStockCommand> violation2 = mock(ConstraintViolation.class);
        when(violation2.getMessage()).thenReturn("번들 항목은 최소 1개 이상이어야 합니다");
        when(validator.validate(emptyItemsCommand)).thenReturn(Set.of(violation2));
        
        assertThatThrownBy(() -> sut.execute(emptyItemsCommand))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("번들 항목");
    }
}