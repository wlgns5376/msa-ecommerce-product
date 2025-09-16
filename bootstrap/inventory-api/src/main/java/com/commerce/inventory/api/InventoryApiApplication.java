package com.commerce.inventory.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 재고 관리 API 애플리케이션
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.commerce.inventory.api",
        "com.commerce.inventory.application",
        "com.commerce.inventory.infrastructure"
})
public class InventoryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApiApplication.class, args);
    }
}