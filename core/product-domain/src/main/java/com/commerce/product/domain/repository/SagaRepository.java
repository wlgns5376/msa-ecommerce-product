package com.commerce.product.domain.repository;

import com.commerce.product.domain.model.saga.BundleReservationSaga;
import com.commerce.product.domain.model.saga.SagaId;

import java.util.Optional;

public interface SagaRepository {
    
    void save(BundleReservationSaga saga);
    
    Optional<BundleReservationSaga> findById(SagaId sagaId);
    
    Optional<BundleReservationSaga> findByReservationId(String reservationId);
    
    void update(BundleReservationSaga saga);
}