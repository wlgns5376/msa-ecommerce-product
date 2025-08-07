package com.commerce.inventory.domain.repository;

import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.domain.model.ReservationStatus;
import com.commerce.inventory.domain.model.SkuId;
import com.commerce.product.domain.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends Repository<Reservation, ReservationId> {
    
    List<Reservation> findBySkuIdAndStatus(SkuId skuId, ReservationStatus status);
    
    List<Reservation> findByOrderId(String orderId);
    
    List<Reservation> findExpiredReservations(LocalDateTime currentTime);
    
    int deleteExpiredReservations(LocalDateTime currentTime);
}