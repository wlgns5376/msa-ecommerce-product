package com.commerce.inventory.application.usecase;

import com.commerce.inventory.application.usecase.ReleaseReservationCommand;
import com.commerce.inventory.application.usecase.ReleaseReservationUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.inventory.domain.exception.InvalidReservationIdException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.application.port.out.LoadReservationPort;
import com.commerce.inventory.application.port.out.SaveReservationPort;
import com.commerce.inventory.domain.model.SkuId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReleaseReservationService implements ReleaseReservationUseCase {
    
    private final LoadReservationPort loadReservationPort;
    private final SaveReservationPort saveReservationPort;
    private final LoadInventoryPort loadInventoryPort;
    private final SaveInventoryPort saveInventoryPort;
    
    @Override
    public void release(ReleaseReservationCommand command) {
        // 1. 예약 조회
        Reservation reservation = findReservationOrThrow(command.getReservationId());
        
        // 2. 예약 상태 검증 및 도메인 로직 수행
        // reservation.release()는 내부적으로 상태 검증과 변경을 모두 수행하므로,
        // 이 호출만으로 '실패-빠르게'가 가능하며 코드도 간결해집니다.
        reservation.release();

        // 3. 재고 조회
        Inventory inventory = findInventoryOrThrow(reservation.getSkuId());

        // 4. 재고의 예약 수량 복원
        inventory.releaseReservedQuantity(reservation.getQuantity());

        // 5. 영속성 처리
        saveReservationPort.save(reservation);
        saveInventoryPort.save(inventory);
    }

    private Reservation findReservationOrThrow(String reservationIdValue) {
        ReservationId reservationId = new ReservationId(reservationIdValue);
        return loadReservationPort.findById(reservationId)
                .orElseThrow(() -> new InvalidReservationIdException(
                        "예약을 찾을 수 없습니다: " + reservationIdValue
                ));
    }

    private Inventory findInventoryOrThrow(SkuId skuId) {
        return loadInventoryPort.load(skuId)
                .orElseThrow(() -> new InvalidInventoryException(
                        "재고를 찾을 수 없습니다: " + skuId.value()
                ));
    }
}