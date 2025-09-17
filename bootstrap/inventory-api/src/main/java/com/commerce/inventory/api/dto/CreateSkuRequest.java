package com.commerce.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SKU 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSkuRequest {

    @NotBlank(message = "SKU 코드는 필수입니다")
    @Size(min = 1, max = 50, message = "SKU 코드는 1-50자 사이여야 합니다")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "SKU 코드는 대문자, 숫자, 하이픈, 언더스코어만 사용 가능합니다")
    private String code;

    @NotBlank(message = "상품명은 필수입니다")
    @Size(min = 1, max = 100, message = "상품명은 1-100자 사이여야 합니다")
    private String name;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;

    @Positive(message = "무게는 양수여야 합니다")
    private Double weight;

    @Pattern(regexp = "^(GRAM|KG|TON)$", message = "무게 단위는 GRAM, KG, TON 중 하나여야 합니다")
    private String weightUnit;

    @Positive(message = "부피는 양수여야 합니다")
    private Double volume;

    @Pattern(regexp = "^(MILLILITER|LITER|CUBIC_METER)$", message = "부피 단위는 MILLILITER, LITER, CUBIC_METER 중 하나여야 합니다")
    private String volumeUnit;

    /**
     * 무게와 무게 단위는 함께 제공되어야 함
     */
    @jakarta.validation.constraints.AssertTrue(message = "무게와 무게 단위는 함께 제공되어야 합니다")
    public boolean isWeightValid() {
        return (weight == null && weightUnit == null) || (weight != null && weightUnit != null);
    }

    /**
     * 부피와 부피 단위는 함께 제공되어야 함
     */
    @jakarta.validation.constraints.AssertTrue(message = "부피와 부피 단위는 함께 제공되어야 합니다")
    public boolean isVolumeValid() {
        return (volume == null && volumeUnit == null) || (volume != null && volumeUnit != null);
    }
}