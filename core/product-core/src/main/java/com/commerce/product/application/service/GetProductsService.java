package com.commerce.product.application.service;

import com.commerce.product.application.usecase.GetProductResponse;
import com.commerce.product.application.usecase.GetProductsRequest;
import com.commerce.product.application.usecase.GetProductsResponse;
import com.commerce.product.application.usecase.GetProductsUseCase;
import com.commerce.product.domain.model.Product;
import com.commerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetProductsService implements GetProductsUseCase {
    
    private final ProductRepository productRepository;
    
    @Override
    @Transactional(readOnly = true)
    public GetProductsResponse execute(GetProductsRequest request) {
        log.debug("Getting products with request: {}", request);
        
        Pageable pageable = createPageable(request);
        Page<Product> productPage = findProducts(request, pageable);
        
        return GetProductsResponse.builder()
                .products(productPage.getContent().stream()
                        .map(this::toGetProductResponse)
                        .collect(Collectors.toList()))
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .pageNumber(productPage.getNumber())
                .pageSize(productPage.getSize())
                .build();
    }
    
    private Pageable createPageable(GetProductsRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // 기본 정렬: 최신순
        
        if (request.getSort() != null && !request.getSort().isEmpty()) {
            String[] sortParams = request.getSort().split(",");
            if (sortParams.length >= 2) {
                Sort.Direction direction = "desc".equalsIgnoreCase(sortParams[1]) 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC;
                sort = Sort.by(direction, sortParams[0]);
            } else if (sortParams.length == 1) {
                sort = Sort.by(Sort.Direction.ASC, sortParams[0]);
            }
        }
        
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
    
    private Page<Product> findProducts(GetProductsRequest request, Pageable pageable) {
        // 검색과 필터 조건을 모두 적용
        if (hasSearchOrFilterCriteria(request)) {
            return productRepository.findBySearchAndFilters(
                    request.getSearch(),
                    request.getType(),
                    request.getStatus(),
                    pageable
            );
        }
        
        // 조건이 없으면 전체 조회
        return productRepository.findAll(pageable);
    }
    
    private boolean hasSearchOrFilterCriteria(GetProductsRequest request) {
        return request.getSearch() != null ||
                request.getType() != null ||
                request.getStatus() != null;
    }
    
    private GetProductResponse toGetProductResponse(Product product) {
        return GetProductResponse.builder()
                .productId(product.getId().value())
                .name(product.getName().value())
                .description(product.getDescription())
                .type(product.getType())
                .status(product.getStatus())
                .createdAt(null) // TODO: Product 도메인에 createdAt 추가 필요
                .updatedAt(null) // TODO: Product 도메인에 updatedAt 추가 필요
                .build();
    }
}