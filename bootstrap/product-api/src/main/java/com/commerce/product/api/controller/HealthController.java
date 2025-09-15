package com.commerce.product.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 헬스 체크 컨트롤러
 */
@RestController
public class HealthController {
    
    @GetMapping("/health/simple")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "product-api");
    }
}