package com.commerce.inventory.application.usecase;

import com.commerce.inventory.application.port.in.ReleaseReservationCommand;
import com.commerce.inventory.application.port.in.ReleaseReservationUseCase;
import com.commerce.inventory.application.port.out.LoadInventoryPort;
import com.commerce.inventory.application.port.out.SaveInventoryPort;
import com.commerce.inventory.domain.exception.InvalidInventoryException;
import com.commerce.inventory.domain.exception.InvalidReservationIdException;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.repository.ReservationRepository;
import com.commerce.inventory.domain.model.SkuId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReleaseReservationService implements ReleaseReservationUseCase {
    
    private final ReservationRepository reservationRepository;
    private final LoadInventoryPort loadInventoryPort;
    private final SaveInventoryPort saveInventoryPort;
    
    @Override
    public void release(ReleaseReservationCommand command) {
        // 1. 엔티티 조회
        Reservation reservation = findReservationOrThrow(command.getReservationId());
        
        // 2. 도메인 로직 수행 - 예약 해제
        reservation.release();
        
        // 3. 재고 조회 및 수량 해제
        Inventory inventory = findInventoryOrThrow(reservation.getSkuId());
        inventory.releaseReservedQuantity(reservation.getQuantity());

        // 4. 영속성 처리
        reservationRepository.save(reservation);
        saveInventoryPort.save(inventory);
    }

    private Reservation findReservationOrThrow(String reservationIdValue) {
        ReservationId reservationId = new ReservationId(reservationIdValue);
        return reservationRepository.findById(reservationId)
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