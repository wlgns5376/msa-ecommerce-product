package com.commerce.product.domain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfiguration {
    
    @Bean(name = "ioExecutor")
    public Executor ioExecutor() {
        return Executors.newFixedThreadPool(100);
    }
}