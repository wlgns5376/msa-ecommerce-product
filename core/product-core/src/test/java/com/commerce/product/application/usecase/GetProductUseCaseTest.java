package com.commerce.product.application.usecase;

import com.commerce.product.domain.exception.ProductNotFoundException;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import com.commerce.product.domain.service.StockAvailabilityService;
import com.commerce.product.domain.service.result.AvailabilityResult;
import com.commerce.product.domain.service.result.BundleAvailabilityResult;
import com.commerce.product.test.helper.ProductTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProductUseCase 테스트")
class GetProductUseCaseTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private StockAvailabilityService stockAvailabilityService;
    
    private GetProductUseCase getProductUseCase;
    
    @BeforeEach
    void setUp() {
        getProductUseCase = new GetProductService(productRepository, stockAvailabilityService, 1L);
    }
    
    @Nested
    @DisplayName("상품 조회 시")
    class GetProductTest {
        
        @Test
        @DisplayName("존재하는 상품ID로 조회하면 상품 정보를 반환한다")
        void should_return_product_when_product_exists() {
            // Given
            ProductId productId = ProductId.of("550e8400-e29b-41d4-a716-446655440001");
            Product product = createNormalProduct(productId);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            
            // 재고 조회 Mock
            given(stockAvailabilityService.checkSingleSkuAvailability("SKU001"))
                .willReturn(CompletableFuture.completedFuture(createAvailableResult()));
            
            // When
            GetProductResponse response = getProductUseCase.execute(new GetProductRequest(productId.value()));
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(productId.value());
            assertThat(response.getName()).isEqualTo("테스트 상품");
            assertThat(response.getType()).isEqualTo(ProductType.NORMAL);
            assertThat(response.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(response.getOptions()).hasSize(1);
            
            // 재고 확인이 호출되었는지 검증
            verify(stockAvailabilityService, times(1)).checkSingleSkuAvailability("SKU001");
        }
        
        @Test
        @DisplayName("존재하지 않는 상품ID로 조회하면 예외가 발생한다")
        void should_throw_exception_when_product_not_exists() {
            // Given
            ProductId productId = ProductId.of("550e8400-e29b-41d4-a716-446655440002");
            given(productRepository.findById(productId)).willReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> getProductUseCase.execute(new GetProductRequest(productId.value())))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId.value());
        }
        
        @Test
        @DisplayName("묶음 상품 조회 시 묶음 재고를 확인한다")
        void should_check_bundle_availability_for_bundle_product() {
            // Given
            ProductId productId = ProductId.of("550e8400-e29b-41d4-a716-446655440003");
            Product bundleProduct = createBundleProduct(productId);
            given(productRepository.findById(productId)).willReturn(Optional.of(bundleProduct));
            
            // 묶음 재고 조회 Mock
            given(stockAvailabilityService.checkBundleAvailability(any(SkuMapping.class)))
                .willReturn(CompletableFuture.completedFuture(createBundleAvailableResult()));
            
            // When
            GetProductResponse response = getProductUseCase.execute(new GetProductRequest(productId.value()));
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getType()).isEqualTo(ProductType.BUNDLE);
            assertThat(response.getOptions()).hasSize(1);
            assertThat(response.getOptions().get(0).getAvailableQuantity()).isEqualTo(10);
            
            // 묶음 재고 확인이 호출되었는지 검증
            verify(stockAvailabilityService, times(1)).checkBundleAvailability(any(SkuMapping.class));
        }
        
        @Test
        @DisplayName("재고가 없는 상품은 가용 수량이 0으로 표시된다")
        void should_show_zero_quantity_when_out_of_stock() {
            // Given
            ProductId productId = ProductId.of("550e8400-e29b-41d4-a716-446655440001");
            Product product = createNormalProduct(productId);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            
            // 재고 없음 Mock
            given(stockAvailabilityService.checkSingleSkuAvailability("SKU001"))
                .willReturn(CompletableFuture.completedFuture(createOutOfStockResult()));
            
            // When
            GetProductResponse response = getProductUseCase.execute(new GetProductRequest(productId.value()));
            
            // Then
            assertThat(response.getOptions().get(0).getAvailableQuantity()).isEqualTo(0);
            assertThat(response.getOptions().get(0).isAvailable()).isFalse();
        }
        
        @Test
        @DisplayName("재고 조회 실패 시에도 상품 정보는 반환한다")
        void should_return_product_info_even_when_stock_check_fails() {
            // Given
            ProductId productId = ProductId.of("550e8400-e29b-41d4-a716-446655440001");
            Product product = createNormalProduct(productId);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            
            // 재고 조회 실패 Mock
            given(stockAvailabilityService.checkSingleSkuAvailability("SKU001"))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("Stock service error")));
            
            // When
            GetProductResponse response = getProductUseCase.execute(new GetProductRequest(productId.value()));
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(productId.value());
            assertThat(response.getOptions().get(0).getAvailableQuantity()).isEqualTo(0);
            assertThat(response.getOptions().get(0).isAvailable()).isFalse();
        }
        
        @Test
        @DisplayName("재고 조회 타임아웃 시에도 상품 정보는 반환한다")
        void should_return_product_info_when_stock_check_times_out() {
            // Given
            ProductId productId = ProductId.of("550e8400-e29b-41d4-a716-446655440001");
            Product product = createNormalProduct(productId);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // 재고 조회 타임아웃 Mock (완료되지 않는 Future)
            given(stockAvailabilityService.checkSingleSkuAvailability("SKU001"))
                .willReturn(new CompletableFuture<>());

            // When
            GetProductResponse response = getProductUseCase.execute(new GetProductRequest(productId.value()));

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(productId.value());
            assertThat(response.getOptions().get(0).getAvailableQuantity()).isEqualTo(0);
            assertThat(response.getOptions().get(0).isAvailable()).isFalse();
        }
        
        @Test
        @DisplayName("여러 옵션 중 일부만 재고 조회가 실패해도 성공한 옵션의 재고는 정확히 표시된다")
        void should_show_correct_stock_for_successful_options_when_some_fail() {
            // Given
            ProductId productId = ProductId.of("550e8400-e29b-41d4-a716-446655440005");
            Product product = createProductWithMultipleOptions(productId);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            
            // 첫 번째 옵션은 성공
            given(stockAvailabilityService.checkSingleSkuAvailability("SKU001"))
                .willReturn(CompletableFuture.completedFuture(new AvailabilityResult(true, 50)));
            
            // 두 번째 옵션은 실패
            given(stockAvailabilityService.checkSingleSkuAvailability("SKU002"))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("Stock service error")));
            
            // 세 번째 옵션은 성공 (지연됨)
            given(stockAvailabilityService.checkSingleSkuAvailability("SKU003"))
                .willReturn(CompletableFuture.supplyAsync(() -> new AvailabilityResult(true, 30),
                    CompletableFuture.delayedExecutor(100, java.util.concurrent.TimeUnit.MILLISECONDS)));
            
            // When
            GetProductResponse response = getProductUseCase.execute(new GetProductRequest(productId.value()));
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOptions()).hasSize(3);
            
            // 첫 번째 옵션: 성공
            assertThat(response.getOptions().get(0).getOptionId()).isEqualTo("option1");
            assertThat(response.getOptions().get(0).isAvailable()).isTrue();
            assertThat(response.getOptions().get(0).getAvailableQuantity()).isEqualTo(50);
            
            // 두 번째 옵션: 실패 (재고 없음으로 표시)
            assertThat(response.getOptions().get(1).getOptionId()).isEqualTo("option2");
            assertThat(response.getOptions().get(1).isAvailable()).isFalse();
            assertThat(response.getOptions().get(1).getAvailableQuantity()).isEqualTo(0);
            
            // 세 번째 옵션: 성공 (지연되었지만 완료됨)
            assertThat(response.getOptions().get(2).getOptionId()).isEqualTo("option3");
            assertThat(response.getOptions().get(2).isAvailable()).isTrue();
            assertThat(response.getOptions().get(2).getAvailableQuantity()).isEqualTo(30);
        }
    }
    
    private Product createNormalProduct(ProductId productId) {
        return ProductTestBuilder.builder()
            .withId(productId)
            .withName("테스트 상품")
            .withDescription("테스트 상품 설명")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withOption(ProductOption.single("기본 옵션", Money.of(10000), "SKU001"))
            .build();
    }
    
    private Product createBundleProduct(ProductId productId) {
        Map<String, Integer> bundleMappings = Map.of("SKU001", 2, "SKU002", 1);
        SkuMapping bundleSkuMapping = SkuMapping.bundle(bundleMappings);
        
        ProductOption bundleOption = ProductOption.bundle(
            "묶음 옵션",
            Money.of(25000),
            bundleSkuMapping
        );
        
        return ProductTestBuilder.builder()
            .withId(productId)
            .withName("묶음 상품")
            .withDescription("묶음 상품 설명")
            .withType(ProductType.BUNDLE)
            .withStatus(ProductStatus.ACTIVE)
            .withOption(bundleOption)
            .build();
    }
    
    private AvailabilityResult createAvailableResult() {
        return new AvailabilityResult(true, 100);
    }
    
    private AvailabilityResult createOutOfStockResult() {
        return new AvailabilityResult(false, 0);
    }
    
    private BundleAvailabilityResult createBundleAvailableResult() {
        List<BundleAvailabilityResult.SkuAvailabilityDetail> details = List.of(
            new BundleAvailabilityResult.SkuAvailabilityDetail("SKU001", 2, 30, 15),
            new BundleAvailabilityResult.SkuAvailabilityDetail("SKU002", 1, 20, 20)
        );
        
        return BundleAvailabilityResult.available(10, details);
    }
    
    private Product createProductWithMultipleOptions(ProductId productId) {
        // 옵션 1
        ProductOption option1 = ProductOption.restore(
            "option1",
            "옵션 1",
            Money.of(10000),
            SkuMapping.single("SKU001")
        );
        
        // 옵션 2
        ProductOption option2 = ProductOption.restore(
            "option2",
            "옵션 2",
            Money.of(15000),
            SkuMapping.single("SKU002")
        );
        
        // 옵션 3
        ProductOption option3 = ProductOption.restore(
            "option3",
            "옵션 3",
            Money.of(20000),
            SkuMapping.single("SKU003")
        );
        
        List<ProductOption> options = List.of(option1, option2, option3);
        
        return ProductTestBuilder.builder()
            .withId(productId)
            .withName("다중 옵션 상품")
            .withDescription("여러 옵션이 있는 상품")
            .withType(ProductType.NORMAL)
            .withStatus(ProductStatus.ACTIVE)
            .withOptions(options)
            .build();
    }
}