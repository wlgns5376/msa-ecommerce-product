package com.commerce.inventory.application.usecase;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.usecase.ReleaseReservationCommand;
import com.commerce.inventory.application.usecase.ReleaseReservationUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.exception.InvalidReservationIdException;
import com.commerce.inventory.domain.exception.InvalidReservationStateException;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.inventory.domain.model.*;
import com.commerce.inventory.application.port.out.LoadReservationPort;
import com.commerce.inventory.application.port.out.SaveReservationPort;
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

    private static final String RESERVATION_ID_VALUE = "RESV123";
    private static final String SKU_ID_VALUE = "SKU001";
    private static final ReservationId RESERVATION_ID = new ReservationId(RESERVATION_ID_VALUE);
    private static final SkuId SKU_ID = new SkuId(SKU_ID_VALUE);
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 1, 1, 12, 0);

    private ReleaseReservationUseCase useCase;

    @Mock
    private LoadReservationPort loadReservationPort;
    
    @Mock
    private SaveReservationPort saveReservationPort;
    
    @Mock
    private LoadInventoryPort loadInventoryPort;
    
    @Mock
    private SaveInventoryPort saveInventoryPort;

    @BeforeEach
    void setUp() {
        useCase = new ReleaseReservationService(
            loadReservationPort,
            saveReservationPort,
            loadInventoryPort,
            saveInventoryPort
        );
    }
    
    private Reservation createDefaultReservation(ReservationId reservationId, SkuId skuId) {
        return Reservation.create(
                reservationId,
                skuId,
                Quantity.of(10),
                "ORDER001",
                FIXED_TIME.plusHours(1),
                FIXED_TIME
        );
    }

    private ReleaseReservationCommand createCommand(String reservationId) {
        return ReleaseReservationCommand.builder()
                .reservationId(reservationId)
                .build();
    }

    @Test
    @DisplayName("정상적으로 예약을 해제할 수 있다")
    void shouldReleaseReservationSuccessfully() {
        // Given
        Reservation reservation = createDefaultReservation(RESERVATION_ID, SKU_ID);
        
        Inventory inventory = Inventory.create(
            SKU_ID,
            Quantity.of(100),
            Quantity.of(50)
        );
        
        given(loadReservationPort.findById(RESERVATION_ID))
            .willReturn(Optional.of(reservation));
        given(loadInventoryPort.load(SKU_ID))
            .willReturn(Optional.of(inventory));
        
        ReleaseReservationCommand command = createCommand(RESERVATION_ID_VALUE);
        
        // When
        useCase.release(command);
        
        // Then
        then(saveReservationPort).should().save(reservation);
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
        
        given(loadReservationPort.findById(reservationId))
            .willReturn(Optional.empty());
        
        ReleaseReservationCommand command = createCommand(reservationIdValue);
        
        // When & Then
        assertThatThrownBy(() -> useCase.release(command))
            .isInstanceOf(InvalidReservationIdException.class)
            .hasMessage("예약을 찾을 수 없습니다: " + reservationIdValue);
        
        then(saveInventoryPort).should(never()).save(any());
        then(saveReservationPort).should(never()).save(any());
    }
    
    @Test
    @DisplayName("이미 해제된 예약을 다시 해제 시도시 예외가 발생한다")
    void shouldThrowExceptionWhenReservationAlreadyReleased() {
        // Given
        Reservation reservation = createDefaultReservation(RESERVATION_ID, SKU_ID);
        reservation.release(); // 이미 해제된 상태
        
        given(loadReservationPort.findById(RESERVATION_ID))
            .willReturn(Optional.of(reservation));
        
        ReleaseReservationCommand command = createCommand(RESERVATION_ID_VALUE);
        
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
        Quantity reservedQuantity = Quantity.of(10);
        
        Reservation reservation = Reservation.create(
            RESERVATION_ID,
            SKU_ID,
            reservedQuantity,
            "ORDER001",
            FIXED_TIME.minusHours(1), // 만료된 예약
            FIXED_TIME.minusHours(2)
        );
        
        Inventory inventory = Inventory.create(
            SKU_ID,
            Quantity.of(100),
            Quantity.of(50)
        );
        
        given(loadReservationPort.findById(RESERVATION_ID))
            .willReturn(Optional.of(reservation));
        given(loadInventoryPort.load(SKU_ID))
            .willReturn(Optional.of(inventory));
        
        ReleaseReservationCommand command = createCommand(RESERVATION_ID_VALUE);
        
        // When
        useCase.release(command);
        
        // Then
        then(saveReservationPort).should().save(reservation);
        then(saveInventoryPort).should().save(inventory);
        
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(40));
    }
    
    @Test
    @DisplayName("재고가 없는 경우 예외가 발생한다")
    void shouldThrowExceptionWhenInventoryNotFound() {
        // Given
        Reservation reservation = createDefaultReservation(RESERVATION_ID, SKU_ID);
        
        given(loadReservationPort.findById(RESERVATION_ID))
            .willReturn(Optional.of(reservation));
        given(loadInventoryPort.load(SKU_ID))
            .willReturn(Optional.empty());
        
        ReleaseReservationCommand command = createCommand(RESERVATION_ID_VALUE);
        
        // When & Then
        assertThatThrownBy(() -> useCase.release(command))
            .isInstanceOf(InvalidInventoryException.class)
            .hasMessage("재고를 찾을 수 없습니다: " + SKU_ID.value());
        
        then(saveInventoryPort).should(never()).save(any());
        then(saveReservationPort).should(never()).save(any());
    }
    
    @Test
    @DisplayName("확정된 예약은 해제할 수 없다")
    void shouldNotReleaseConfirmedReservation() {
        // Given
        Reservation reservation = createDefaultReservation(RESERVATION_ID, SKU_ID);
        reservation.confirm(FIXED_TIME); // 확정된 상태
        
        given(loadReservationPort.findById(RESERVATION_ID))
            .willReturn(Optional.of(reservation));
        
        ReleaseReservationCommand command = createCommand(RESERVATION_ID_VALUE);
        
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
        Reservation reservation = createDefaultReservation(RESERVATION_ID, SKU_ID);
        
        // 현재 예약된 수량(5)이 해제하려는 수량(10)보다 적은 재고 생성
        Inventory inventory = Inventory.create(
            SKU_ID,
            Quantity.of(100),
            Quantity.of(5) // 5개만 예약된 상태
        );
        
        given(loadReservationPort.findById(RESERVATION_ID))
            .willReturn(Optional.of(reservation));
        given(loadInventoryPort.load(SKU_ID))
            .willReturn(Optional.of(inventory));
        
        ReleaseReservationCommand command = createCommand(RESERVATION_ID_VALUE);
        
        // When & Then
        assertThatThrownBy(() -> useCase.release(command))
            .isInstanceOf(InvalidInventoryException.class)
            .hasMessage("해제할 예약 수량이 부족합니다. 현재 예약: 5, 해제 요청: 10");
        
        then(saveInventoryPort).should(never()).save(any());
        then(saveReservationPort).should(never()).save(any());
    }
}