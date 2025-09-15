package com.commerce.inventory.domain.model;

import com.commerce.inventory.domain.exception.InvalidSkuCodeException;
import com.commerce.common.domain.model.ValueObject;

import java.util.regex.Pattern;

public record SkuCode(String value) implements ValueObject {
    
    private static final Pattern VALID_SKU_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9\\-_]+$");
    
    public SkuCode {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidSkuCodeException("SKU 코드는 필수입니다");
        }
        
        if (!VALID_SKU_CODE_PATTERN.matcher(value).matches()) {
            throw new InvalidSkuCodeException("SKU 코드는 영문자, 숫자, 하이픈, 언더스코어만 허용됩니다: " + value);
        }
    }
    
    public static SkuCode of(String value) {
        return new SkuCode(value);
    }
}