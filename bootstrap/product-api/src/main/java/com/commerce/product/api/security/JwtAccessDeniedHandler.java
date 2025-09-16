package com.commerce.product.api.security;

import com.commerce.product.api.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * JWT 권한이 없을 때 처리하는 Handler
 */
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
        
        log.error("접근 권한이 없는 사용자 접근: {}", accessDeniedException.getMessage());
        
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpServletResponse.SC_FORBIDDEN)
                .error("Forbidden")
                .message("접근 권한이 없습니다.")
                .code("ACCESS_DENIED")
                .timestamp(LocalDateTime.now())
                .build();
        
        OBJECT_MAPPER.writeValue(response.getOutputStream(), errorResponse);
    }
}