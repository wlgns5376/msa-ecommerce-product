package com.commerce.inventory.api.config;

import com.commerce.common.event.DomainEventPublisher;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 설정 클래스
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public CreateSkuUseCase createSkuUseCase() {
        return mock(CreateSkuUseCase.class);
    }
    
    @Bean
    @Primary
    public DomainEventPublisher domainEventPublisher() {
        return mock(DomainEventPublisher.class);
    }
}