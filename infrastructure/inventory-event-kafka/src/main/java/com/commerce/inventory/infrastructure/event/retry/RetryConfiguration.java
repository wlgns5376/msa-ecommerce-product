package com.commerce.inventory.infrastructure.event.retry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 재시도 설정
 */
@Configuration
@ConfigurationProperties(prefix = "kafka.retry")
public class RetryConfiguration {
    
    private int maxAttempts = 3;
    private long backoffMillis = 1000;
    private double backoffMultiplier = 2.0;
    private long maxBackoffMillis = 30000;
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public long getBackoffMillis() {
        return backoffMillis;
    }
    
    public void setBackoffMillis(long backoffMillis) {
        this.backoffMillis = backoffMillis;
    }
    
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }
    
    public long getMaxBackoffMillis() {
        return maxBackoffMillis;
    }
    
    public void setMaxBackoffMillis(long maxBackoffMillis) {
        this.maxBackoffMillis = maxBackoffMillis;
    }
}