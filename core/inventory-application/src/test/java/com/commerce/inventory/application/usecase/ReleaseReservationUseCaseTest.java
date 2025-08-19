package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.port.in.ReleaseReservationCommand;
import com.commerce.inventory.application.port.in.ReleaseReservationUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.exception.InvalidReservationIdException;
import com.commerce.inventory.domain.exception.InvalidReservationStateException;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.domain.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseReservationUseCase 테스트")
class ReleaseReservationUseCaseTest {

    private ReleaseReservationUseCase useCase;

    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private LoadInventoryPort loadInventoryPort;
    
    @Mock
    private SaveInventoryPort saveInventoryPort;

    @BeforeEach
    void setUp() {
        useCase = new ReleaseReservationService(
            reservationRepository,
            loadInventoryPort,
            saveInventoryPort
        );
    }

    @Test
    @DisplayName("정상적으로 예약을 해제할 수 있다")
    void shouldReleaseReservationSuccessfully() {
        // Given
        String reservationIdValue = "RESV123";
        ReservationId reservationId = new ReservationId(reservationIdValue);
        SkuId skuId = new SkuId("SKU001");
        Quantity reservedQuantity = Quantity.of(10);
        
        Reservation reservation = Reservation.create(
            reservationId,
            skuId,
            reservedQuantity,
            "ORDER001",
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        );
        
        Inventory inventory = Inventory.create(
            skuId,
            Quantity.of(100),
            Quantity.of(50)
        );
        
        given(reservationRepository.findById(reservationId))
            .willReturn(Optional.of(reservation));
        given(loadInventoryPort.load(skuId))
            .willReturn(Optional.of(inventory));
        
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
            .reservationId(reservationIdValue)
            .build();
        
        // When
        useCase.release(command);
        
        // Then
        then(reservationRepository).should().save(reservation);
        then(saveInventoryPort).should().save(inventory);
        
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(40));
    }
    
    @Test
    @DisplayName("존재하지 않는 예약 ID로 해제 시도시 예외가 발생한다")
    void shouldThrowExceptionWhenReservationNotFound() {
        // Given
        String reservationIdValue = "NOT_EXISTS";
        ReservationId reservationId = new ReservationId(reservationIdValue);
        
        given(reservationRepository.findById(reservationId))
            .willReturn(Optional.empty());
        
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
            .reservationId(reservationIdValue)
            .build();
        
        // When & Then
        assertThatThrownBy(() -> useCase.release(command))
            .isInstanceOf(InvalidReservationIdException.class)
            .hasMessage("예약을 찾을 수 없습니다: " + reservationIdValue);
        
        then(saveInventoryPort).should(never()).save(any());
        then(reservationRepository).should(never()).save(any());
    }
    
    @Test
    @DisplayName("이미 해제된 예약을 다시 해제 시도시 예외가 발생한다")
    void shouldThrowExceptionWhenReservationAlreadyReleased() {
        // Given
        String reservationIdValue = "RESV123";
        ReservationId reservationId = new ReservationId(reservationIdValue);
        SkuId skuId = new SkuId("SKU001");
        
        Reservation reservation = Reservation.create(
            reservationId,
            skuId,
            Quantity.of(10),
            "ORDER001",
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        );
        reservation.release(); // 이미 해제된 상태
        
        given(reservationRepository.findById(reservationId))
            .willReturn(Optional.of(reservation));
        
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
            .reservationId(reservationIdValue)
            .build();
        
        // When & Then
        assertThatThrownBy(() -> useCase.release(command))
            .isInstanceOf(InvalidReservationStateException.class)
            .hasMessage("이미 해제된 예약입니다");
        
        then(loadInventoryPort).should(never()).load(any());
        then(saveInventoryPort).should(never()).save(any());
    }
    
    @Test
    @DisplayName("만료된 예약도 해제할 수 있다")
    void shouldReleaseExpiredReservation() {
        // Given
        String reservationIdValue = "RESV123";
        ReservationId reservationId = new ReservationId(reservationIdValue);
        SkuId skuId = new SkuId("SKU001");
        Quantity reservedQuantity = Quantity.of(10);
        
        Reservation reservation = Reservation.create(
            reservationId,
            skuId,
            reservedQuantity,
            "ORDER001",
            LocalDateTime.now().minusHours(1), // 만료된 예약
            LocalDateTime.now().minusHours(2)
        );
        
        Inventory inventory = Inventory.create(
            skuId,
            Quantity.of(100),
            Quantity.of(50)
        );
        
        given(reservationRepository.findById(reservationId))
            .willReturn(Optional.of(reservation));
        given(loadInventoryPort.load(skuId))
            .willReturn(Optional.of(inventory));
        
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
            .reservationId(reservationIdValue)
            .build();
        
        // When
        useCase.release(command);
        
        // Then
        then(reservationRepository).should().save(reservation);
        then(saveInventoryPort).should().save(inventory);
        
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(40));
    }
    
    @Test
    @DisplayName("재고가 없는 경우 예외가 발생한다")
    void shouldThrowExceptionWhenInventoryNotFound() {
        // Given
        String reservationIdValue = "RESV123";
        ReservationId reservationId = new ReservationId(reservationIdValue);
        SkuId skuId = new SkuId("SKU001");
        
        Reservation reservation = Reservation.create(
            reservationId,
            skuId,
            Quantity.of(10),
            "ORDER001",
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        );
        
        given(reservationRepository.findById(reservationId))
            .willReturn(Optional.of(reservation));
        given(loadInventoryPort.load(skuId))
            .willReturn(Optional.empty());
        
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
            .reservationId(reservationIdValue)
            .build();
        
        // When & Then
        assertThatThrownBy(() -> useCase.release(command))
            .isInstanceOf(InvalidInventoryException.class)
            .hasMessage("재고를 찾을 수 없습니다: " + skuId.value());
        
        then(saveInventoryPort).should(never()).save(any());
        then(reservationRepository).should(never()).save(any());
    }
    
    @Test
    @DisplayName("확정된 예약은 해제할 수 없다")
    void shouldNotReleaseConfirmedReservation() {
        // Given
        String reservationIdValue = "RESV123";
        ReservationId reservationId = new ReservationId(reservationIdValue);
        SkuId skuId = new SkuId("SKU001");
        
        Reservation reservation = Reservation.create(
            reservationId,
            skuId,
            Quantity.of(10),
            "ORDER001",
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        );
        reservation.confirm(LocalDateTime.now()); // 확정된 상태
        
        given(reservationRepository.findById(reservationId))
            .willReturn(Optional.of(reservation));
        
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
            .reservationId(reservationIdValue)
            .build();
        
        // When & Then
        assertThatThrownBy(() -> useCase.release(command))
            .isInstanceOf(InvalidReservationStateException.class)
            .hasMessage("확정된 예약은 해제할 수 없습니다");
        
        then(loadInventoryPort).should(never()).load(any());
        then(saveInventoryPort).should(never()).save(any());
    }
    
    @Test
    @DisplayName("예약된 재고가 부족할 때 예외가 발생한다")
    void shouldThrowExceptionWhenReservedStockIsInsufficient() {
        // Given
        String reservationIdValue = "RESV123";
        ReservationId reservationId = new ReservationId(reservationIdValue);
        SkuId skuId = new SkuId("SKU001");
        
        Reservation reservation = Reservation.create(
            reservationId,
            skuId,
            Quantity.of(10),
            "ORDER001",
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        );
        
        // 현재 예약된 수량(5)이 해제하려는 수량(10)보다 적은 재고 생성
        Inventory inventory = Inventory.create(
            skuId,
            Quantity.of(100),
            Quantity.of(5) // 5개만 예약된 상태
        );
        
        given(reservationRepository.findById(reservationId))
            .willReturn(Optional.of(reservation));
        given(loadInventoryPort.load(skuId))
            .willReturn(Optional.of(inventory));
        
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
            .reservationId(reservationIdValue)
            .build();
        
        // When & Then
        assertThatThrownBy(() -> useCase.release(command))
            .isInstanceOf(InvalidInventoryException.class)
            .hasMessage("해제할 예약 수량이 부족합니다. 현재 예약: 5, 해제 요청: 10");
        
        then(saveInventoryPort).should(never()).save(any());
        then(reservationRepository).should(never()).save(any());
    }
}