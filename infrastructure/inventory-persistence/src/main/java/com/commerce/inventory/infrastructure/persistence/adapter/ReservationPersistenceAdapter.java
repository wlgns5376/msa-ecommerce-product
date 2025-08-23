package com.commerce.inventory.infrastructure.persistence.adapter;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.service.port.out.LoadReservationPort;
import com.commerce.inventory.application.service.port.out.SaveReservationPort;
import com.commerce.inventory.domain.model.*;
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
                .map(this::toDomainEntity);
    }
    
    @Override
    public List<Reservation> findAllById(List<ReservationId> ids) {
        List<String> idValues = ids.stream()
                .map(ReservationId::value)
                .collect(Collectors.toList());
        
        return reservationJpaRepository.findAllByIdIn(idValues).stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Reservation> findByOrderIdAndInventoryId(String orderId, String inventoryId) {
        return reservationJpaRepository.findByOrderIdAndInventoryId(orderId, inventoryId)
                .map(this::toDomainEntity);
    }
    
    @Override
    public List<Reservation> findByOrderId(String orderId) {
        return reservationJpaRepository.findByOrderId(orderId).stream()
                .map(this::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity jpaEntity = toJpaEntity(reservation);
        ReservationJpaEntity savedEntity = reservationJpaRepository.save(jpaEntity);
        return toDomainEntity(savedEntity);
    }
    
    private Reservation toDomainEntity(ReservationJpaEntity jpaEntity) {
        return Reservation.restore(
                new ReservationId(jpaEntity.getId()),
                new SkuId(jpaEntity.getSkuId()),
                Quantity.of(jpaEntity.getQuantity()),
                jpaEntity.getOrderId(),
                jpaEntity.getExpiresAt(),
                mapToDomainStatus(jpaEntity.getStatus()),
                jpaEntity.getCreatedAt(),
                jpaEntity.getVersion()
        );
    }
    
    private ReservationJpaEntity toJpaEntity(Reservation domainEntity) {
        return ReservationJpaEntity.builder()
                .id(domainEntity.getId().value())
                .skuId(domainEntity.getSkuId().value())
                .inventoryId(domainEntity.getSkuId().value()) // SKU ID를 inventory ID로 사용
                .quantity(domainEntity.getQuantity().value())
                .orderId(domainEntity.getOrderId())
                .expiresAt(domainEntity.getExpiresAt())
                .status(mapToJpaStatus(domainEntity.getStatus()))
                .createdAt(domainEntity.getCreatedAt())
                .updatedAt(domainEntity.getUpdatedAt())
                .version(domainEntity.getVersion())
                .build();
    }
    
    private ReservationStatus mapToDomainStatus(ReservationJpaEntity.ReservationStatus jpaStatus) {
        return switch (jpaStatus) {
            case ACTIVE -> ReservationStatus.ACTIVE;
            case CONFIRMED -> ReservationStatus.CONFIRMED;
            case RELEASED -> ReservationStatus.RELEASED;
            case EXPIRED -> ReservationStatus.EXPIRED;
        };
    }
    
    private ReservationJpaEntity.ReservationStatus mapToJpaStatus(ReservationStatus domainStatus) {
        return switch (domainStatus) {
            case ACTIVE -> ReservationJpaEntity.ReservationStatus.ACTIVE;
            case CONFIRMED -> ReservationJpaEntity.ReservationStatus.CONFIRMED;
            case RELEASED -> ReservationJpaEntity.ReservationStatus.RELEASED;
            case EXPIRED -> ReservationJpaEntity.ReservationStatus.EXPIRED;
        };
    }
}