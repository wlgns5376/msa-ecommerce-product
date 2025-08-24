package com.commerce.product.application.usecase;

import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.model.ProductOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class GetProductResponse {
    private final String id;
    private final String name;
    private final String description;
    private final String type;
    private final String status;
    private final List<OptionDetail> options;
    
    public static GetProductResponse from(Product product) {
        List<OptionDetail> optionDetails = product.getOptions().stream()
            .map(OptionDetail::from)
            .collect(Collectors.toList());
            
        return GetProductResponse.builder()
            .id(product.getId().toString())
            .name(product.getName().value())
            .description(product.getDescription())
            .type(product.getType().name())
            .status(product.getStatus().name())
            .options(optionDetails)
            .build();
    }
    
    @Getter
    @Builder
    @AllArgsConstructor
    public static class OptionDetail {
        private final String id;
        private final String name;
        private final BigDecimal price;
        private final String currency;
        private final List<SkuMappingDetail> skuMappings;
        
        public static OptionDetail from(ProductOption option) {
            List<SkuMappingDetail> mappingDetails = option.getSkuMapping().mappings().entrySet().stream()
                .map(entry -> SkuMappingDetail.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
                
            return OptionDetail.builder()
                .id(option.getId())
                .name(option.getName())
                .price(option.getPrice().amount())
                .currency(option.getPrice().currency().name())
                .skuMappings(mappingDetails)
                .build();
        }
    }
    
    @Getter
    @AllArgsConstructor
    public static class SkuMappingDetail {
        private final String skuId;
        private final int quantity;

        public static SkuMappingDetail of(String skuId, int quantity) {
            return new SkuMappingDetail(skuId, quantity);
        }
    }
}