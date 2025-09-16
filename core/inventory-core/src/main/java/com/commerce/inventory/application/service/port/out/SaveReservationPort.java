package com.commerce.inventory.application.service.port.out;

import com.commerce.inventory.domain.model.Reservation;

public interface SaveReservationPort {
    Reservation save(Reservation reservation);
}