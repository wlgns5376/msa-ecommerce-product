package com.commerce.product.test.helper;

import com.commerce.product.domain.model.*;
import java.util.ArrayList;
import java.util.List;

public class ProductTestBuilder {
    private ProductId id;
    private ProductName name;
    private String description;
    private ProductType type;
    private ProductStatus status;
    private List<ProductOption> options = new ArrayList<>();
    private List<CategoryId> categoryIds = new ArrayList<>();
    private boolean outOfStock = false;
    private Long version = 0L;

    private ProductTestBuilder() {
        // 기본값 설정
        this.id = ProductId.generate();
        this.name = ProductName.of("테스트 상품");
        this.description = "테스트 상품 설명";
        this.type = ProductType.NORMAL;
        this.status = ProductStatus.DRAFT;
    }

    public static ProductTestBuilder builder() {
        return new ProductTestBuilder();
    }

    public ProductTestBuilder withId(ProductId id) {
        this.id = id;
        return this;
    }

    public ProductTestBuilder withName(String name) {
        this.name = ProductName.of(name);
        return this;
    }

    public ProductTestBuilder withName(ProductName name) {
        this.name = name;
        return this;
    }

    public ProductTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ProductTestBuilder withType(ProductType type) {
        this.type = type;
        return this;
    }

    public ProductTestBuilder withStatus(ProductStatus status) {
        this.status = status;
        return this;
    }

    public ProductTestBuilder withOption(ProductOption option) {
        this.options.add(option);
        return this;
    }

    public ProductTestBuilder withOptions(List<ProductOption> options) {
        this.options.addAll(options);
        return this;
    }

    public ProductTestBuilder withCategoryId(CategoryId categoryId) {
        this.categoryIds.add(categoryId);
        return this;
    }

    public ProductTestBuilder withCategoryIds(List<CategoryId> categoryIds) {
        this.categoryIds.addAll(categoryIds);
        return this;
    }

    public ProductTestBuilder withOutOfStock(boolean outOfStock) {
        this.outOfStock = outOfStock;
        return this;
    }

    public ProductTestBuilder withVersion(Long version) {
        this.version = version;
        return this;
    }

    public Product build() {
        // restore 메소드를 사용하여 원하는 상태로 복원
        return Product.restore(id, name, description, type, status, options, categoryIds, outOfStock, version);
    }

    // 자주 사용되는 상태의 Product를 생성하는 헬퍼 메소드들
    public static Product createActiveProduct() {
        return builder()
            .withStatus(ProductStatus.ACTIVE)
            .withOption(ProductOption.single("기본 옵션", Money.of(10000), "SKU001"))
            .build();
    }

    public static Product createActiveProduct(ProductId id) {
        return builder()
            .withId(id)
            .withStatus(ProductStatus.ACTIVE)
            .withOption(ProductOption.single("기본 옵션", Money.of(10000), "SKU001"))
            .build();
    }

    public static Product createDraftProduct() {
        return builder()
            .withStatus(ProductStatus.DRAFT)
            .build();
    }

    public static Product createBundleProduct() {
        return builder()
            .withType(ProductType.BUNDLE)
            .withStatus(ProductStatus.ACTIVE)
            .build();
    }

    public static Product createBundleProduct(ProductId id) {
        return builder()
            .withId(id)
            .withType(ProductType.BUNDLE)
            .withStatus(ProductStatus.ACTIVE)
            .build();
    }
}
