package com.commerce.product.api.config;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.product.domain.repository.InventoryRepository;
import com.commerce.product.domain.repository.LockRepository;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.repository.SagaRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import com.commerce.product.domain.service.impl.StockAvailabilityServiceImpl;
import com.commerce.product.domain.service.saga.BundleReservationSagaOrchestrator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

@Configuration
public class ServiceConfig {
    
    @Bean
    public StockAvailabilityService stockAvailabilityService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            LockRepository lockRepository,
            SagaRepository sagaRepository,
            DomainEventPublisher eventPublisher,
            BundleReservationSagaOrchestrator bundleReservationSagaOrchestrator,
            @Qualifier("ioTaskExecutor") Executor ioTaskExecutor) {
        return new StockAvailabilityServiceImpl(
                inventoryRepository,
                productRepository,
                lockRepository,
                sagaRepository,
                eventPublisher,
                bundleReservationSagaOrchestrator,
                ioTaskExecutor
        );
    }
}
