package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.inventory.application.service.port.out.LoadReservationPort;
import com.commerce.inventory.application.service.port.out.SaveReservationPort;
import com.commerce.inventory.domain.model.Reservation;
import com.commerce.inventory.domain.model.ReservationId;
import com.commerce.inventory.infrastructure.persistence.entity.ReservationJpaEntity;
import com.commerce.inventory.infrastructure.persistence.repository.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationPersistenceAdapter implements LoadReservationPort, SaveReservationPort {
    
    private final ReservationJpaRepository reservationJpaRepository;
    
    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return reservationJpaRepository.findById(id.value())
                .map(ReservationJpaEntity::toDomainModel);
    }
    
    @Override
    public List<Reservation> findAllById(List<ReservationId> ids) {
        List<String> idValues = ids.stream()
                .map(ReservationId::value)
                .collect(Collectors.toList());
        
        return reservationJpaRepository.findAllByIdIn(idValues).stream()
                .map(ReservationJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Reservation> findByOrderIdAndInventoryId(String orderId, String inventoryId) {
        return reservationJpaRepository.findByOrderIdAndInventoryId(orderId, inventoryId)
                .map(ReservationJpaEntity::toDomainModel);
    }
    
    @Override
    public List<Reservation> findByOrderId(String orderId) {
        return reservationJpaRepository.findByOrderId(orderId).stream()
                .map(ReservationJpaEntity::toDomainModel)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity jpaEntity = ReservationJpaEntity.fromDomainModel(reservation);
        ReservationJpaEntity savedEntity = reservationJpaRepository.save(jpaEntity);
        return savedEntity.toDomainModel();
    }
}