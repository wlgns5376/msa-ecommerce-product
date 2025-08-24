package com.commerce.product.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExecutorConfig {
    
    @Value("${executor.io-task.core-pool-size:10}")
    private int corePoolSize;
    
    @Value("${executor.io-task.max-pool-size:50}")
    private int maxPoolSize;
    
    @Value("${executor.io-task.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${executor.io-task.thread-name-prefix:io-task-}")
    private String threadNamePrefix;
    
    @Bean(name = "ioTaskExecutor")
    public Executor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}
