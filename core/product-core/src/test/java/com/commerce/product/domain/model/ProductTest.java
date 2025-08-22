package com.commerce.product.domain.model;

import com.commerce.product.domain.event.*;
import com.commerce.product.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private ProductName productName;
    private String description;

    @BeforeEach
    void setUp() {
        productName = new ProductName("맥북 프로 16인치");
        description = "2023년형 M2 Pro 칩셋 탑재";
    }

    @Test
    @DisplayName("새로운 상품을 생성할 수 있다")
    void shouldCreateNewProduct() {
        // When
        Product product = Product.create(productName, description, ProductType.NORMAL);

        // Then
        assertThat(product).isNotNull();
        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo(productName);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getType()).isEqualTo(ProductType.NORMAL);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getOptions()).isEmpty();
        assertThat(product.getCategoryIds()).isEmpty();
        assertThat(product.isOutOfStock()).isFalse();
    }

    @Test
    @DisplayName("description이 null인 경우 빈 문자열로 처리된다")
    void shouldHandleNullDescription() {
        // When
        Product product = Product.create(productName, null, ProductType.NORMAL);

        // Then
        assertThat(product.getDescription()).isEqualTo("");
    }

    @ParameterizedTest
    @EnumSource(ProductType.class)
    @DisplayName("모든 상품 타입으로 상품을 생성할 수 있다")
    void shouldCreateProductWithAllTypes(ProductType type) {
        // When
        Product product = Product.create(productName, description, type);

        // Then
        assertThat(product.getType()).isEqualTo(type);
    }

    @Test
    @DisplayName("상품 생성 시 ProductCreatedEvent가 발생한다")
    void shouldRaiseProductCreatedEventWhenCreated() {
        // When
        Product product = Product.create(productName, description, ProductType.NORMAL);

        // Then
        assertThat(product.getDomainEvents()).hasSize(1);
        assertThat(product.getDomainEvents().get(0)).isInstanceOf(ProductCreatedEvent.class);
        
        ProductCreatedEvent event = (ProductCreatedEvent) product.getDomainEvents().get(0);
        assertThat(event.getProductId()).isEqualTo(product.getId());
        assertThat(event.getName()).isEqualTo(productName.value());
        assertThat(event.getType()).isEqualTo(ProductType.NORMAL);
    }

    @Test
    @DisplayName("기존 ID로 상품을 복원할 수 있다")
    void shouldRestoreProductWithExistingId() {
        // Given
        ProductId existingId = new ProductId(UUID.randomUUID().toString());

        // When
        Product product = new Product(existingId, productName, description, ProductType.NORMAL);

        // Then
        assertThat(product.getId()).isEqualTo(existingId);
        assertThat(product.getDomainEvents()).isEmpty(); // 복원 시에는 이벤트 발생하지 않음
    }

    @Test
    @DisplayName("일반 상품에 단일 SKU 옵션을 추가할 수 있다")
    void shouldAddSingleSkuOptionToNormalProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        ProductOption option = ProductOption.single(
                "블랙 - 512GB",
                Money.of(new BigDecimal("3500000"), Currency.KRW),
                "SKU001"
        );

        // When
        product.addOption(option);

        // Then
        assertThat(product.getOptions()).hasSize(1);
        assertThat(product.getOptions()).contains(option);
        assertThat(product.getDomainEvents()).hasSize(2); // Created + OptionAdded
        assertThat(product.getDomainEvents().get(1)).isInstanceOf(ProductOptionAddedEvent.class);
    }

    @Test
    @DisplayName("일반 상품에 여러 옵션을 추가할 수 있다")
    void shouldAddMultipleOptionsToNormalProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        ProductOption option1 = ProductOption.single(
                "실버 - 512GB",
                Money.of(new BigDecimal("3500000"), Currency.KRW),
                "SKU001"
        );
        ProductOption option2 = ProductOption.single(
                "스페이스 그레이 - 1TB",
                Money.of(new BigDecimal("4500000"), Currency.KRW),
                "SKU002"
        );

        // When
        product.addOption(option1);
        product.addOption(option2);

        // Then
        assertThat(product.getOptions()).hasSize(2);
        assertThat(product.getOptions()).containsExactly(option1, option2);
    }

    @Test
    @DisplayName("묶음 상품에는 복수 SKU 옵션만 추가할 수 있다")
    void shouldOnlyAddMultipleSkuOptionToBundleProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.BUNDLE);
        Map<String, Integer> bundleMapping = new HashMap<>();
        bundleMapping.put("SKU001", 1);
        bundleMapping.put("SKU002", 1);
        
        ProductOption bundleOption = ProductOption.bundle(
                "맥북 + 매직마우스 번들",
                Money.of(new BigDecimal("3800000"), Currency.KRW),
                SkuMapping.bundle(bundleMapping)
        );

        // When
        product.addOption(bundleOption);

        // Then
        assertThat(product.getOptions()).hasSize(1);
        assertThat(product.getOptions().get(0).isBundle()).isTrue();
    }

    @Test
    @DisplayName("묶음 상품에 단일 SKU 옵션을 추가하면 예외가 발생한다")
    void shouldThrowExceptionWhenAddingSingleSkuOptionToBundleProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.BUNDLE);
        ProductOption singleSkuOption = ProductOption.single(
                "단일 상품 옵션",
                Money.of(new BigDecimal("1000000"), Currency.KRW),
                "SKU001"
        );

        // When & Then
        assertThatThrownBy(() -> product.addOption(singleSkuOption))
                .isInstanceOf(InvalidOptionException.class)
                .hasMessageContaining("번들 상품은 번들 옵션만 가질 수 있습니다");
    }

    @Test
    @DisplayName("이름이 동일한 옵션을 추가하면 예외가 발생한다")
    void shouldThrowExceptionWhenAddingOptionWithDuplicateName() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        ProductOption option1 = ProductOption.single(
                "블랙 - 512GB",
                Money.of(new BigDecimal("3500000"), Currency.KRW),
                "SKU001"
        );
        ProductOption option2 = ProductOption.single(
                "블랙 - 512GB", // 동일한 이름
                Money.of(new BigDecimal("3600000"), Currency.KRW), // 다른 가격
                "SKU002" // 다른 SKU
        );
        product.addOption(option1);

        // When & Then
        assertThatThrownBy(() -> product.addOption(option2))
                .isInstanceOf(DuplicateOptionException.class)
                .hasMessageContaining("동일한 이름의 옵션이 이미 존재합니다");
    }

    @Test
    @DisplayName("상품 정보를 수정할 수 있다")
    void shouldUpdateProductInfo() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        ProductName newName = new ProductName("맥북 프로 14인치");
        String newDescription = "2023년형 M2 Max 칩셋 탑재";

        // When
        product.update(newName, newDescription);

        // Then
        assertThat(product.getName()).isEqualTo(newName);
        assertThat(product.getDescription()).isEqualTo(newDescription);
        assertThat(product.getDomainEvents()).hasSize(2); // Created + Updated
        assertThat(product.getDomainEvents().get(1)).isInstanceOf(ProductUpdatedEvent.class);
    }

    @Test
    @DisplayName("옵션이 있는 상품을 활성화할 수 있다")
    void shouldActivateProductWithOptions() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.addOption(ProductOption.single(
                "기본 옵션",
                Money.of(new BigDecimal("1000000"), Currency.KRW),
                "SKU001"
        ));

        // When
        product.activate();

        // Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getDomainEvents()).hasSize(3); // Created + OptionAdded + Activated
        assertThat(product.getDomainEvents().get(2)).isInstanceOf(ProductActivatedEvent.class);
    }

    @Test
    @DisplayName("옵션이 없는 상품을 활성화하면 예외가 발생한다")
    void shouldThrowExceptionWhenActivatingProductWithoutOptions() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);

        // When & Then
        assertThatThrownBy(() -> product.activate())
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("상품을 활성화하려면 최소 하나의 옵션이 필요합니다");
    }

    @Test
    @DisplayName("활성화된 상품을 비활성화할 수 있다")
    void shouldDeactivateActiveProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.addOption(ProductOption.single(
                "기본 옵션",
                Money.of(new BigDecimal("1000000"), Currency.KRW),
                "SKU001"
        ));
        product.activate();

        // When
        product.deactivate();

        // Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(product.getDomainEvents()).hasSize(4); // Created + OptionAdded + Activated + Deactivated
        assertThat(product.getDomainEvents().get(3)).isInstanceOf(ProductDeactivatedEvent.class);
    }

    @Test
    @DisplayName("상품을 삭제할 수 있다")
    void shouldDeleteProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);

        // When
        product.delete();

        // Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
        assertThat(product.getDomainEvents()).hasSize(2); // Created + Deleted
        assertThat(product.getDomainEvents().get(1)).isInstanceOf(ProductDeletedEvent.class);
    }

    @Test
    @DisplayName("삭제된 상품에 옵션을 추가하면 예외가 발생한다")
    void shouldThrowExceptionWhenAddingOptionToDeletedProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.delete();
        ProductOption option = ProductOption.single(
                "테스트 옵션",
                Money.of(new BigDecimal("1000000"), Currency.KRW),
                "SKU001"
        );

        // When & Then
        assertThatThrownBy(() -> product.addOption(option))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("삭제된 상품에는 옵션을 추가할 수 없습니다");
    }

    @Test
    @DisplayName("삭제된 상품을 수정하면 예외가 발생한다")
    void shouldThrowExceptionWhenUpdatingDeletedProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.delete();

        // When & Then
        assertThatThrownBy(() -> product.update(new ProductName("새이름"), "새설명"))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("삭제된 상품은 수정할 수 없습니다");
    }

    @Test
    @DisplayName("상품에 카테고리를 할당할 수 있다")
    void shouldAssignCategoriesToProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        List<CategoryId> categoryIds = Arrays.asList(
                new CategoryId(UUID.randomUUID().toString()),
                new CategoryId(UUID.randomUUID().toString())
        );

        // When
        product.assignCategories(categoryIds);

        // Then
        assertThat(product.getCategoryIds()).hasSize(2);
        assertThat(product.getCategoryIds()).containsExactlyElementsOf(categoryIds);
    }

    @Test
    @DisplayName("상품에 5개를 초과하는 카테고리를 할당하면 예외가 발생한다")
    void shouldThrowExceptionWhenAssigningMoreThanFiveCategories() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        List<CategoryId> categoryIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            categoryIds.add(new CategoryId(UUID.randomUUID().toString()));
        }

        // When & Then
        assertThatThrownBy(() -> product.assignCategories(categoryIds))
                .isInstanceOf(MaxCategoryLimitException.class)
                .hasMessageContaining("상품은 최대 5개의 카테고리에만 할당할 수 있습니다");
    }

    @Test
    @DisplayName("상품을 품절 상태로 변경할 수 있다")
    void shouldMarkProductAsOutOfStock() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);

        // When
        product.markAsOutOfStock();

        // Then
        assertThat(product.isOutOfStock()).isTrue();
        assertThat(product.getDomainEvents()).hasSize(2); // Created + OutOfStock
        assertThat(product.getDomainEvents().get(1)).isInstanceOf(ProductOutOfStockEvent.class);
    }

    @Test
    @DisplayName("품절 상품을 재고 있음 상태로 변경할 수 있다")
    void shouldMarkProductAsInStock() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.markAsOutOfStock();

        // When
        product.markAsInStock();

        // Then
        assertThat(product.isOutOfStock()).isFalse();
        assertThat(product.getDomainEvents()).hasSize(3); // Created + OutOfStock + InStock
        assertThat(product.getDomainEvents().get(2)).isInstanceOf(ProductInStockEvent.class);
    }

    @Test
    @DisplayName("동일한 ID를 가진 Product는 equals 비교시 true를 반환한다")
    void shouldReturnTrueWhenComparingProductsWithSameId() {
        // Given
        ProductId id = new ProductId(UUID.randomUUID().toString());
        Product product1 = new Product(id, productName, description, ProductType.NORMAL);
        Product product2 = new Product(id, new ProductName("다른이름"), "다른설명", ProductType.BUNDLE);

        // When & Then
        assertThat(product1).isEqualTo(product2);
        assertThat(product1.hashCode()).isEqualTo(product2.hashCode());
    }

    @Test
    @DisplayName("다른 ID를 가진 Product는 equals 비교시 false를 반환한다")
    void shouldReturnFalseWhenComparingProductsWithDifferentIds() {
        // Given
        Product product1 = Product.create(productName, description, ProductType.NORMAL);
        Product product2 = Product.create(productName, description, ProductType.NORMAL);

        // When & Then
        assertThat(product1).isNotEqualTo(product2);
    }

    @Test
    @DisplayName("상품 옵션은 불변 리스트로 반환된다")
    void shouldReturnImmutableOptionsList() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.addOption(ProductOption.single(
                "테스트 옵션",
                Money.of(new BigDecimal("1000000"), Currency.KRW),
                "SKU001"
        ));

        // When
        List<ProductOption> options = product.getOptions();

        // Then
        assertThatThrownBy(() -> options.add(ProductOption.single(
                "새 옵션",
                Money.of(new BigDecimal("2000000"), Currency.KRW),
                "SKU002"
        )))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("카테고리 ID 리스트는 불변 리스트로 반환된다")
    void shouldReturnImmutableCategoryIdsList() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        List<CategoryId> categoryIds = Arrays.asList(
                new CategoryId(UUID.randomUUID().toString())
        );
        product.assignCategories(categoryIds);

        // When
        List<CategoryId> retrievedCategoryIds = product.getCategoryIds();

        // Then
        assertThatThrownBy(() -> retrievedCategoryIds.add(new CategoryId(UUID.randomUUID().toString())))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}