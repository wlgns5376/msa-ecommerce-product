package com.commerce.product.domain.model;

import com.commerce.product.domain.exception.InvalidOptionException;
import com.commerce.product.domain.exception.InvalidProductException;
import com.commerce.product.domain.exception.MaxCategoryLimitException;
import com.commerce.product.domain.exception.DuplicateOptionException;
import com.commerce.product.common.event.DomainEvent;
import com.commerce.product.domain.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private ProductId productId;
    private ProductName productName;
    private String description;

    @BeforeEach
    void setUp() {
        productId = ProductId.generate();
        productName = new ProductName("맥북 프로 16인치");
        description = "애플 최신 맥북 프로";
    }

    @Test
    @DisplayName("일반 상품을 생성할 수 있다")
    void shouldCreateNormalProduct() {
        // When
        Product product = Product.create(
                productName,
                description,
                ProductType.NORMAL
        );

        // Then
        assertThat(product).isNotNull();
        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo(productName);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getType()).isEqualTo(ProductType.NORMAL);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getOptions()).isEmpty();
        assertThat(product.getDomainEvents()).hasSize(1);
        assertThat(product.getDomainEvents().get(0)).isInstanceOf(ProductCreatedEvent.class);
    }

    @Test
    @DisplayName("묶음 상품을 생성할 수 있다")
    void shouldCreateBundleProduct() {
        // When
        Product product = Product.create(
                productName,
                description,
                ProductType.BUNDLE
        );

        // Then
        assertThat(product.getType()).isEqualTo(ProductType.BUNDLE);
    }

    @Test
    @DisplayName("필수 정보가 없으면 상품을 생성할 수 없다")
    void shouldThrowExceptionWhenRequiredFieldIsNull() {
        // When & Then
        assertThatThrownBy(() -> Product.create(null, description, ProductType.NORMAL))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("Product name is required");

        assertThatThrownBy(() -> Product.create(productName, description, null))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("Product type is required");
    }

    @Test
    @DisplayName("일반 상품에 단일 SKU 옵션을 추가할 수 있다")
    void shouldAddSingleSkuOptionToNormalProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        ProductOption option = ProductOption.create(
                "블랙 - 512GB",
                new Money(new BigDecimal("3500000")),
                Arrays.asList(new SkuMapping("SKU001", 1))
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
        ProductOption option1 = ProductOption.create(
                "실버 - 512GB",
                new Money(new BigDecimal("3500000")),
                Arrays.asList(new SkuMapping("SKU001", 1))
        );
        ProductOption option2 = ProductOption.create(
                "스페이스 그레이 - 1TB",
                new Money(new BigDecimal("4500000")),
                Arrays.asList(new SkuMapping("SKU002", 1))
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
        ProductOption bundleOption = ProductOption.create(
                "맥북 + 매직마우스 번들",
                new Money(new BigDecimal("3800000")),
                Arrays.asList(
                        new SkuMapping("SKU001", 1),
                        new SkuMapping("SKU002", 1)
                )
        );

        // When
        product.addOption(bundleOption);

        // Then
        assertThat(product.getOptions()).hasSize(1);
        assertThat(product.getOptions().get(0).hasMultipleSkus()).isTrue();
    }

    @Test
    @DisplayName("묶음 상품에 단일 SKU 옵션을 추가하면 예외가 발생한다")
    void shouldThrowExceptionWhenAddingSingleSkuOptionToBundleProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.BUNDLE);
        ProductOption singleSkuOption = ProductOption.create(
                "단일 상품 옵션",
                new Money(new BigDecimal("1000000")),
                Arrays.asList(new SkuMapping("SKU001", 1))
        );

        // When & Then
        assertThatThrownBy(() -> product.addOption(singleSkuOption))
                .isInstanceOf(InvalidOptionException.class)
                .hasMessageContaining("Bundle product must have options with multiple SKUs");
    }

    @Test
    @DisplayName("중복된 옵션을 추가하면 예외가 발생한다")
    void shouldThrowExceptionWhenAddingDuplicateOption() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        ProductOption option = ProductOption.create(
                "블랙 - 512GB",
                new Money(new BigDecimal("3500000")),
                Arrays.asList(new SkuMapping("SKU001", 1))
        );
        product.addOption(option);

        // When & Then
        assertThatThrownBy(() -> product.addOption(option))
                .isInstanceOf(DuplicateOptionException.class)
                .hasMessageContaining("Option already exists");
    }

    @Test
    @DisplayName("상품 정보를 수정할 수 있다")
    void shouldUpdateProductInfo() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        ProductName newName = new ProductName("맥북 프로 16인치 M3");
        String newDescription = "애플 최신 M3 칩셋 탑재 맥북 프로";

        // When
        product.update(newName, newDescription);

        // Then
        assertThat(product.getName()).isEqualTo(newName);
        assertThat(product.getDescription()).isEqualTo(newDescription);
        assertThat(product.getDomainEvents()).hasSize(2); // Created + Updated
        assertThat(product.getDomainEvents().get(1)).isInstanceOf(ProductUpdatedEvent.class);
    }

    @Test
    @DisplayName("상품을 활성화할 수 있다")
    void shouldActivateProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.addOption(ProductOption.create(
                "기본 옵션",
                new Money(new BigDecimal("1000000")),
                Arrays.asList(new SkuMapping("SKU001", 1))
        ));

        // When
        product.activate();

        // Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getDomainEvents()).hasSize(3); // Created + OptionAdded + Activated
        assertThat(product.getDomainEvents().get(2)).isInstanceOf(ProductActivatedEvent.class);
    }

    @Test
    @DisplayName("옵션이 없는 상품은 활성화할 수 없다")
    void shouldThrowExceptionWhenActivatingProductWithoutOptions() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);

        // When & Then
        assertThatThrownBy(() -> product.activate())
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("Product must have at least one option to be activated");
    }

    @Test
    @DisplayName("상품을 비활성화할 수 있다")
    void shouldDeactivateProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.addOption(ProductOption.create(
                "기본 옵션",
                new Money(new BigDecimal("1000000")),
                Arrays.asList(new SkuMapping("SKU001", 1))
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
    @DisplayName("삭제된 상품은 수정할 수 없다")
    void shouldThrowExceptionWhenUpdatingDeletedProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.delete();

        // When & Then
        assertThatThrownBy(() -> product.update(new ProductName("새 이름"), "새 설명"))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("Cannot update deleted product");
    }

    @Test
    @DisplayName("상품에 카테고리를 연결할 수 있다")
    void shouldAssignCategoriesToProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        List<CategoryId> categoryIds = Arrays.asList(
                new CategoryId("CAT001"),
                new CategoryId("CAT002")
        );

        // When
        product.assignCategories(categoryIds);

        // Then
        assertThat(product.getCategoryIds()).hasSize(2);
        assertThat(product.getCategoryIds()).containsExactlyElementsOf(categoryIds);
    }

    @Test
    @DisplayName("상품에 최대 5개까지 카테고리를 연결할 수 있다")
    void shouldAssignUpTo5CategoriesToProduct() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        List<CategoryId> categoryIds = Arrays.asList(
                new CategoryId("CAT001"),
                new CategoryId("CAT002"),
                new CategoryId("CAT003"),
                new CategoryId("CAT004"),
                new CategoryId("CAT005")
        );

        // When
        product.assignCategories(categoryIds);

        // Then
        assertThat(product.getCategoryIds()).hasSize(5);
    }

    @Test
    @DisplayName("상품에 5개를 초과하는 카테고리를 연결하면 예외가 발생한다")
    void shouldThrowExceptionWhenAssigningMoreThan5Categories() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        List<CategoryId> categoryIds = Arrays.asList(
                new CategoryId("CAT001"),
                new CategoryId("CAT002"),
                new CategoryId("CAT003"),
                new CategoryId("CAT004"),
                new CategoryId("CAT005"),
                new CategoryId("CAT006")
        );

        // When & Then
        assertThatThrownBy(() -> product.assignCategories(categoryIds))
                .isInstanceOf(MaxCategoryLimitException.class)
                .hasMessageContaining("Product can be assigned to maximum 5 categories");
    }

    @Test
    @DisplayName("재고 부족 상태로 변경할 수 있다")
    void shouldMarkAsOutOfStock() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.addOption(ProductOption.create(
                "기본 옵션",
                new Money(new BigDecimal("1000000")),
                Arrays.asList(new SkuMapping("SKU001", 1))
        ));
        product.activate();

        // When
        product.markAsOutOfStock();

        // Then
        assertThat(product.isOutOfStock()).isTrue();
        assertThat(product.getDomainEvents()).hasSize(4); // Created + OptionAdded + Activated + OutOfStock
        assertThat(product.getDomainEvents().get(3)).isInstanceOf(ProductOutOfStockEvent.class);
    }

    @Test
    @DisplayName("재고 보충 상태로 변경할 수 있다")
    void shouldMarkAsInStock() {
        // Given
        Product product = Product.create(productName, description, ProductType.NORMAL);
        product.addOption(ProductOption.create(
                "기본 옵션",
                new Money(new BigDecimal("1000000")),
                Arrays.asList(new SkuMapping("SKU001", 1))
        ));
        product.activate();
        product.markAsOutOfStock();

        // When
        product.markAsInStock();

        // Then
        assertThat(product.isOutOfStock()).isFalse();
        assertThat(product.getDomainEvents()).hasSize(5); // Created + OptionAdded + Activated + OutOfStock + InStock
        assertThat(product.getDomainEvents().get(4)).isInstanceOf(ProductInStockEvent.class);
    }
}