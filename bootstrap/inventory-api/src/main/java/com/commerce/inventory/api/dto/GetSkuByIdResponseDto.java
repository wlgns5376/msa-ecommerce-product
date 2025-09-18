package com.commerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SKU 조회 응답")
public class GetSkuByIdResponseDto {
    
    @Schema(description = "SKU ID", example = "SKU-001")
    private String id;
    
    @Schema(description = "SKU 코드", example = "TEST-SKU-001")
    private String code;
    
    @Schema(description = "SKU 이름", example = "테스트 SKU")
    private String name;
    
    @Schema(description = "SKU 설명", example = "테스트용 SKU입니다")
    private String description;
    
    @Schema(description = "무게(kg)", example = "1.5")
    private BigDecimal weight;
    
    @Schema(description = "부피(㎥)", example = "10.0")
    private BigDecimal volume;
    
    @Schema(description = "생성 일시", example = "2024-01-01T10:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시", example = "2024-01-01T10:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "버전", example = "1")
    private Long version;
}