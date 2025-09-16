package com.commerce.inventory.application.service.port.out;

import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;

import java.util.List;
import java.util.Optional;

public interface LoadReservationPort {
    Optional<Reservation> findById(ReservationId id);
    List<Reservation> findAllById(List<ReservationId> ids);
    Optional<Reservation> findByOrderIdAndInventoryId(String orderId, String inventoryId);
    List<Reservation> findByOrderId(String orderId);
}