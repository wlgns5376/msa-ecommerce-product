package com.commerce.boilerplate.infrastructure.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA 설정 클래스
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.commerce.boilerplate.infrastructure.persistence"
)
@EntityScan(
    basePackages = "com.commerce.boilerplate.infrastructure.persistence"
)
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
}