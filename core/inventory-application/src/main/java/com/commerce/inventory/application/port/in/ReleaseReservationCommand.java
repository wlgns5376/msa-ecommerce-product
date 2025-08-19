package com.commerce.inventory.application.port.in;

import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Builder
public class ReleaseReservationCommand {
    
    @NotBlank(message = "예약 ID는 필수입니다")
    private final String reservationId;
    
    public ReleaseReservationCommand(String reservationId) {
        this.reservationId = reservationId;
    }
}