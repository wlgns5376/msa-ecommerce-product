package com.commerce.product.application.usecase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCategoryTreeRequest {
    
    @Builder.Default
    private boolean includeInactive = false;
}