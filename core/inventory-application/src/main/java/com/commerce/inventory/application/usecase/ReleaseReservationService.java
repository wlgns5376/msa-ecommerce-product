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
        ReservationId reservationId = new ReservationId(command.getReservationId());
        
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new InvalidReservationIdException(
                "예약을 찾을 수 없습니다: " + command.getReservationId()
            ));
        
        reservation.release();
        
        Inventory inventory = loadInventoryPort.load(reservation.getSkuId())
            .orElseThrow(() -> new InvalidInventoryException(
                "재고를 찾을 수 없습니다: " + reservation.getSkuId().value()
            ));
        
        inventory.releaseReservedQuantity(reservation.getQuantity());
        
        reservationRepository.save(reservation);
        saveInventoryPort.save(inventory);
    }
}