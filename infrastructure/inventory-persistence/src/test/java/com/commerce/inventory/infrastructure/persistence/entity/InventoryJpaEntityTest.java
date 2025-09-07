package com.commerce.inventory.infrastructure.persistence.entity;

import com.commerce.common.domain.model.Quantity;
import com.commerce.inventory.domain.model.Inventory;
import com.commerce.inventory.domain.model.SkuId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryJpaEntityTest {

    @Test
    @DisplayName("도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void should_convert_from_domain_model() {
        // given
        SkuId skuId = SkuId.of("SKU-123");
        Quantity totalQuantity = Quantity.of(100);
        Quantity reservedQuantity = Quantity.of(20);
        Long version = 1L;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        
        Inventory inventory = Inventory.restore(
                skuId,
                totalQuantity,
                reservedQuantity,
                version,
                createdAt,
                updatedAt
        );

        // when
        InventoryJpaEntity entity = InventoryJpaEntity.fromDomainModel(inventory);

        // then
        assertThat(entity.getSkuId()).isEqualTo(skuId.value());
        assertThat(entity.getTotalQuantity()).isEqualTo(totalQuantity.value());
        assertThat(entity.getReservedQuantity()).isEqualTo(reservedQuantity.value());
        assertThat(entity.getVersion()).isEqualTo(version);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("JPA 엔티티를 도메인 모델로 변환할 수 있다")
    void should_convert_to_domain_model() {
        // given
        String skuId = "SKU-123";
        Integer totalQuantity = 100;
        Integer reservedQuantity = 20;
        Long version = 1L;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        
        InventoryJpaEntity entity = InventoryJpaEntity.builder()
                .skuId(skuId)
                .totalQuantity(totalQuantity)
                .reservedQuantity(reservedQuantity)
                .version(version)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // when
        Inventory inventory = entity.toDomainModel();

        // then
        assertThat(inventory.getSkuId()).isEqualTo(SkuId.of(skuId));
        assertThat(inventory.getTotalQuantity()).isEqualTo(Quantity.of(totalQuantity));
        assertThat(inventory.getReservedQuantity()).isEqualTo(Quantity.of(reservedQuantity));
        assertThat(inventory.getVersion()).isEqualTo(version);
        assertThat(inventory.getCreatedAt()).isEqualTo(createdAt);
        assertThat(inventory.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("신규 재고 생성시 도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void should_convert_new_inventory_from_domain_model() {
        // given
        SkuId skuId = SkuId.of("SKU-456");
        Inventory inventory = Inventory.createEmpty(skuId);

        // when
        InventoryJpaEntity entity = InventoryJpaEntity.fromDomainModel(inventory);

        // then
        assertThat(entity.getSkuId()).isEqualTo(skuId.value());
        assertThat(entity.getTotalQuantity()).isEqualTo(0);
        assertThat(entity.getReservedQuantity()).isEqualTo(0);
        assertThat(entity.getVersion()).isEqualTo(0L);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("재고 수량이 변경된 도메인 모델을 JPA 엔티티로 변환할 수 있다")
    void should_convert_updated_inventory_from_domain_model() {
        // given
        SkuId skuId = SkuId.of("SKU-789");
        Inventory inventory = Inventory.createWithInitialStock(skuId, Quantity.of(50));
        inventory.reserve(Quantity.of(10), "ORDER-123", 3600);

        // when
        InventoryJpaEntity entity = InventoryJpaEntity.fromDomainModel(inventory);

        // then
        assertThat(entity.getSkuId()).isEqualTo(skuId.value());
        assertThat(entity.getTotalQuantity()).isEqualTo(50);
        assertThat(entity.getReservedQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("가용 재고가 계산된 도메인 모델을 정확히 복원할 수 있다")
    void should_correctly_restore_domain_model_with_available_quantity() {
        // given
        InventoryJpaEntity entity = InventoryJpaEntity.builder()
                .skuId("SKU-999")
                .totalQuantity(100)
                .reservedQuantity(30)
                .version(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when
        Inventory inventory = entity.toDomainModel();

        // then
        assertThat(inventory.getAvailableQuantity()).isEqualTo(Quantity.of(70));
    }
}