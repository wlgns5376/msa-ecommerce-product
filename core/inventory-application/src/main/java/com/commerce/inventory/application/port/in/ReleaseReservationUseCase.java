package com.commerce.inventory.application.port.in;

public interface ReleaseReservationUseCase {
    void release(ReleaseReservationCommand command);
}