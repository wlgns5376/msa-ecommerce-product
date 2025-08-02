package com.commerce.product.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 에러 응답 클래스
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private final int status;
    private final String error;
    private final String message;
    private final String code;
    private final LocalDateTime timestamp;
    private final Map<String, String> details;
}