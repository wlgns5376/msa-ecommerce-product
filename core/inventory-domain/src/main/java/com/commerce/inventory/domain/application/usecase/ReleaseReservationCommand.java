package com.commerce.inventory.domain.application.usecase;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Getter
@Builder
@RequiredArgsConstructor
public class ReleaseReservationCommand {
    
    @NotBlank(message = "예약 ID는 필수입니다")
    private final String reservationId;
}