package com.commerce.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductStatusTest {

    @Test
    @DisplayName("ProductStatus enum은 모든 상태값을 가진다")
    void shouldHaveAllStatusValues() {
        // When & Then
        assertThat(ProductStatus.values()).hasSize(4);
        assertThat(ProductStatus.valueOf("DRAFT")).isEqualTo(ProductStatus.DRAFT);
        assertThat(ProductStatus.valueOf("ACTIVE")).isEqualTo(ProductStatus.ACTIVE);
        assertThat(ProductStatus.valueOf("INACTIVE")).isEqualTo(ProductStatus.INACTIVE);
        assertThat(ProductStatus.valueOf("DELETED")).isEqualTo(ProductStatus.DELETED);
    }

    @Test
    @DisplayName("DRAFT 상태는 임시저장 상태를 나타낸다")
    void shouldRepresentDraftStatus() {
        // Given
        ProductStatus status = ProductStatus.DRAFT;

        // When & Then
        assertThat(status).isEqualTo(ProductStatus.DRAFT);
        assertThat(status.name()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("ACTIVE 상태는 활성 상태를 나타낸다")
    void shouldRepresentActiveStatus() {
        // Given
        ProductStatus status = ProductStatus.ACTIVE;

        // When & Then
        assertThat(status).isEqualTo(ProductStatus.ACTIVE);
        assertThat(status.name()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("INACTIVE 상태는 비활성 상태를 나타낸다")
    void shouldRepresentInactiveStatus() {
        // Given
        ProductStatus status = ProductStatus.INACTIVE;

        // When & Then
        assertThat(status).isEqualTo(ProductStatus.INACTIVE);
        assertThat(status.name()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("DELETED 상태는 삭제 상태를 나타낸다")
    void shouldRepresentDeletedStatus() {
        // Given
        ProductStatus status = ProductStatus.DELETED;

        // When & Then
        assertThat(status).isEqualTo(ProductStatus.DELETED);
        assertThat(status.name()).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("ProductStatus는 문자열로 변환 가능하다")
    void shouldConvertToString() {
        // When & Then
        assertThat(ProductStatus.DRAFT.toString()).isEqualTo("DRAFT");
        assertThat(ProductStatus.ACTIVE.toString()).isEqualTo("ACTIVE");
        assertThat(ProductStatus.INACTIVE.toString()).isEqualTo("INACTIVE");
        assertThat(ProductStatus.DELETED.toString()).isEqualTo("DELETED");
    }
}