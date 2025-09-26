package com.commerce.product.api.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "카테고리 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    
    @Schema(description = "카테고리 ID", example = "cat-001")
    private String categoryId;
    
    @Schema(description = "카테고리 이름", example = "Electronics")
    private String name;
    
    @Schema(description = "부모 카테고리 ID", example = "cat-000", nullable = true)
    private String parentId;
    
    @Schema(description = "카테고리 레벨 (1: 대분류, 2: 중분류, 3: 소분류)", example = "1", minimum = "1", maximum = "3")
    private int level;
    
    @Schema(description = "정렬 순서", example = "1", minimum = "0")
    private int sortOrder;
    
    @Schema(description = "활성화 여부", example = "true")
    private boolean active;
    
    @Schema(description = "전체 경로", example = "/Electronics/Smartphones")
    private String fullPath;
}