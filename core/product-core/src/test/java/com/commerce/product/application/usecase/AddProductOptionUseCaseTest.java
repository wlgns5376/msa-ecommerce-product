package com.commerce.product.application.usecase;

import com.commerce.product.application.factory.ProductOptionFactory;
import com.commerce.product.application.service.AddProductOptionService;
import com.commerce.product.domain.exception.*;
import com.commerce.product.domain.model.*;
import com.commerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddProductOptionUseCaseTest {

    @Mock
    private ProductRepository productRepository;


    private AddProductOptionUseCase useCase;

    @BeforeEach
    void setUp() {
        ProductOptionFactory productOptionFactory = new ProductOptionFactory();
        useCase = new AddProductOptionService(productRepository, productOptionFactory);
    }

    private AddProductOptionRequest createDefaultRequest(String productId) {
        Map<String, Integer> skuMappings = Map.of("SKU001", 1);
        return AddProductOptionRequest.builder()
                .productId(productId)
                .optionName("블랙 - L")
                .price(BigDecimal.valueOf(29900))
                .currency("KRW")
                .skuMappings(skuMappings)
                .build();
    }

    @Test
    @DisplayName("정상적인 상품 옵션 추가")
    void shouldAddProductOption() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("테스트 상품"),
            "테스트 설명",
            ProductType.NORMAL
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        AddProductOptionRequest request = createDefaultRequest(productId.value());

        // When
        AddProductOptionResponse response = useCase.addProductOption(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(productId.value());
        assertThat(response.getOptionName()).isEqualTo("블랙 - L");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getOptions()).hasSize(1);
        assertThat(savedProduct.getOptions().get(0).getName()).isEqualTo("블랙 - L");

    }

    @Test
    @DisplayName("묶음 상품에 묶음 옵션 추가")
    void shouldAddBundleOptionToBundleProduct() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("묶음 상품"),
            "묶음 상품 설명",
            ProductType.BUNDLE
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Map<String, Integer> skuMappings = Map.of("SKU001", 2, "SKU002", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("A+B 세트")
            .price(BigDecimal.valueOf(39900))
            .currency("KRW")
            .skuMappings(skuMappings)
            .build();

        // When
        AddProductOptionResponse response = useCase.addProductOption(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOptionName()).isEqualTo("A+B 세트");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getOptions()).hasSize(1);
        assertThat(savedProduct.getOptions().get(0).isBundle()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상품에 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        String productId = ProductId.generate().value();
        when(productRepository.findById(any(ProductId.class))).thenReturn(Optional.empty());

        AddProductOptionRequest request = createDefaultRequest(productId);

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(InvalidProductException.class)
            .hasMessage("상품을 찾을 수 없습니다");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("삭제된 상품에 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenAddingOptionToDeletedProduct() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("삭제된 상품"),
            "삭제된 상품 설명",
            ProductType.NORMAL
        );
        product.delete();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AddProductOptionRequest request = createDefaultRequest(productId.value());

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(InvalidProductException.class)
            .hasMessage("삭제된 상품에는 옵션을 추가할 수 없습니다");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("묶음 상품에 단일 SKU 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenAddingSingleSkuOptionToBundleProduct() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("묶음 상품"),
            "묶음 상품 설명",
            ProductType.BUNDLE
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Map<String, Integer> skuMappings = Map.of("SKU001", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("단일 옵션")
            .price(BigDecimal.valueOf(29900))
            .currency("KRW")
            .skuMappings(skuMappings)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(InvalidOptionException.class)
            .hasMessage("번들 상품은 번들 옵션만 가질 수 있습니다");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복된 이름의 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenAddingDuplicateOption() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("테스트 상품"),
            "테스트 설명",
            ProductType.NORMAL
        );

        // 기존 옵션 추가
        Map<String, Integer> existingSkuMappings = Map.of("SKU001", 1);
        ProductOption existingOption = ProductOption.single(
            "블랙 - L",
            new Money(BigDecimal.valueOf(29900), Currency.KRW),
            SkuMapping.of(existingSkuMappings)
        );
        product.addOption(existingOption);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Map<String, Integer> skuMappings = Map.of("SKU002", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("블랙 - L")
            .price(BigDecimal.valueOf(35000))
            .currency("KRW")
            .skuMappings(skuMappings)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(DuplicateOptionException.class)
            .hasMessage("동일한 이름의 옵션이 이미 존재합니다");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("유효하지 않은 가격으로 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenAddingOptionWithInvalidPrice() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("테스트 상품"),
            "테스트 설명",
            ProductType.NORMAL
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Map<String, Integer> skuMappings = Map.of("SKU001", 1);
        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("블랙 - L")
            .price(BigDecimal.valueOf(-1000))
            .currency("KRW")
            .skuMappings(skuMappings)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(InvalidMoneyException.class)
            .hasMessage("Amount cannot be negative");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("SKU 매핑이 없는 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenAddingOptionWithoutSkuMappings() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("테스트 상품"),
            "테스트 설명",
            ProductType.NORMAL
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("블랙 - L")
            .price(BigDecimal.valueOf(29900))
            .currency("KRW")
            .skuMappings(Map.of())
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(InvalidSkuMappingException.class)
            .hasMessage("SKU mappings cannot be null or empty");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("유효하지 않은 SKU 수량으로 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenAddingOptionWithInvalidSkuQuantity() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("테스트 상품"),
            "테스트 설명",
            ProductType.NORMAL
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Map<String, Integer> skuMappings = Map.of("SKU001", 0);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("블랙 - L")
            .price(BigDecimal.valueOf(29900))
            .currency("KRW")
            .skuMappings(skuMappings)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(InvalidSkuMappingException.class)
            .hasMessage("Quantity must be positive for SKU: SKU001");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 통화로 옵션 추가")
    void shouldAddOptionWithDifferentCurrency() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("테스트 상품"),
            "테스트 설명",
            ProductType.NORMAL
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Map<String, Integer> skuMappings = Map.of("SKU001", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("블랙 - L")
            .price(BigDecimal.valueOf(29.99))
            .currency("USD")
            .skuMappings(skuMappings)
            .build();

        // When
        AddProductOptionResponse response = useCase.addProductOption(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOptionName()).isEqualTo("블랙 - L");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getOptions()).hasSize(1);
        assertThat(savedProduct.getOptions().get(0).getPrice().currency()).isEqualTo(Currency.USD);
    }

    @Test
    @DisplayName("유효하지 않은 통화로 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenAddingOptionWithInvalidCurrency() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("테스트 상품"),
            "테스트 설명",
            ProductType.NORMAL
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Map<String, Integer> skuMappings = Map.of("SKU001", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("블랙 - L")
            .price(BigDecimal.valueOf(29900))
            .currency("INVALID_CURRENCY")
            .skuMappings(skuMappings)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(InvalidProductOptionException.class)
            .hasMessage("유효하지 않은 통화입니다: INVALID_CURRENCY");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("일반 상품에 묶음 SKU 옵션 추가 시 예외 발생")
    void shouldThrowExceptionWhenAddingBundleOptionToNormalProduct() {
        // Given
        ProductId productId = ProductId.generate();
        Product product = new Product(
            productId,
            new ProductName("일반 상품"),
            "일반 상품 설명",
            ProductType.NORMAL
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Map<String, Integer> skuMappings = Map.of("SKU001", 2, "SKU002", 1);

        AddProductOptionRequest request = AddProductOptionRequest.builder()
            .productId(productId.value())
            .optionName("묶음 옵션")
            .price(BigDecimal.valueOf(29900))
            .currency("KRW")
            .skuMappings(skuMappings)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.addProductOption(request))
            .isInstanceOf(InvalidOptionException.class)
            .hasMessage("일반 상품은 번들 옵션을 가질 수 없습니다");

        verify(productRepository, never()).save(any());
    }
}