package com.commerce.product.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 상품 API 서비스 메인 애플리케이션 클래스
 */
@SpringBootApplication(scanBasePackages = "com.commerce.product")
public class ApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}