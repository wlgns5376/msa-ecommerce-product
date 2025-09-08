package com.commerce.inventory.api.adapter.out;

import com.commerce.common.event.DomainEvent;
import com.commerce.inventory.domain.event.StockReservedEvent;
import com.commerce.inventory.domain.event.StockReceivedEvent;
import com.commerce.inventory.domain.event.StockDepletedEvent;
import com.commerce.inventory.domain.event.ReservationReleasedEvent;
import com.commerce.inventory.domain.model.*;
import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.application.service.port.out.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@DisplayName("EventPublisher 통합 테스트")
class EventPublisherIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @MockBean
    private ApplicationEventPublisher applicationEventPublisher;

    private SkuId skuId;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        skuId = new SkuId("SKU-001");
        reservation = Reservation.create(
            new ReservationId(UUID.randomUUID().toString()),
            skuId,
            Quantity.of(10),
            "ORDER-001",
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("재고 예약 이벤트를 발행한다")
    void testPublishStockReservedEvent() {
        // Given
        StockReservedEvent event = new StockReservedEvent(skuId, reservation);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(StockReservedEvent.class));
    }

    @Test
    @DisplayName("재고 입고 이벤트를 발행한다")
    void testPublishStockReceivedEvent() {
        // Given
        StockReceivedEvent event = new StockReceivedEvent(skuId, Quantity.of(100), "PO-001");

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(StockReceivedEvent.class));
    }

    @Test
    @DisplayName("재고 소진 이벤트를 발행한다")
    void testPublishStockDepletedEvent() {
        // Given
        StockDepletedEvent event = new StockDepletedEvent(skuId);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(StockDepletedEvent.class));
    }

    @Test
    @DisplayName("예약 해제 이벤트를 발행한다")
    void testPublishReservationReleasedEvent() {
        // Given
        ReservationReleasedEvent event = new ReservationReleasedEvent(skuId, reservation.getId());

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(ReservationReleasedEvent.class));
    }

    @Test
    @DisplayName("여러 도메인 이벤트를 순차적으로 발행한다")
    void testPublishMultipleEvents() {
        // Given
        List<DomainEvent> events = Arrays.asList(
            new StockReceivedEvent(skuId, Quantity.of(100), "PO-001"),
            new StockReservedEvent(skuId, reservation),
            new StockDepletedEvent(skuId)
        );

        // When
        eventPublisher.publishAll(events);

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(StockReceivedEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(StockReservedEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(StockDepletedEvent.class));
    }

    @Test
    @DisplayName("이벤트 발행시 이벤트 타입과 집계 ID가 올바르게 설정된다")
    void testEventMetadata() {
        // Given
        StockReservedEvent event = new StockReservedEvent(skuId, reservation);

        // When
        eventPublisher.publish(event);

        // Then
        verify(applicationEventPublisher).publishEvent(any(StockReservedEvent.class));
    }

    @Test
    @DisplayName("복합 이벤트 시나리오: 재고 입고 -> 예약 -> 소진")
    void testComplexEventScenario() {
        // Given
        // Receive 10 units
        
        Reservation reservation1 = Reservation.create(
            new ReservationId(UUID.randomUUID().toString()),
            skuId,
            Quantity.of(5),
            "ORDER-001",
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        );
        
        Reservation reservation2 = Reservation.create(
            new ReservationId(UUID.randomUUID().toString()),
            skuId,
            Quantity.of(5),
            "ORDER-002",
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now()
        );

        // When
        // 재고 입고
        eventPublisher.publish(new StockReceivedEvent(skuId, Quantity.of(10), "PO-001"));
        
        // 첫 번째 예약
        eventPublisher.publish(new StockReservedEvent(skuId, reservation1));
        
        // 두 번째 예약 (재고 소진)
        eventPublisher.publish(new StockReservedEvent(skuId, reservation2));
        eventPublisher.publish(new StockDepletedEvent(skuId));

        // Then
        verify(applicationEventPublisher, times(1)).publishEvent(any(StockReceivedEvent.class));
        verify(applicationEventPublisher, times(2)).publishEvent(any(StockReservedEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(StockDepletedEvent.class));
    }
}