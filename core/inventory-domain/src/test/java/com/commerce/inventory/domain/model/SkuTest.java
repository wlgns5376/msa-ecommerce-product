package com.commerce.inventory.domain.model;

import com.commerce.common.domain.model.Quantity;

import com.commerce.inventory.domain.exception.InvalidSkuException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuTest {
    
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 1, 1, 12, 0);

    @Test
    @DisplayName("유효한 값으로 SKU를 생성할 수 있다")
    void shouldCreateSkuWithValidValues() {
        // given
        SkuId id = SkuId.generate();
        SkuCode code = new SkuCode("TSHIRT-BLACK-L");
        String name = "티셔츠 - 블랙 - L";
        String description = "블랙 색상 티셔츠 L 사이즈";
        Weight weight = new Weight(250.0, WeightUnit.GRAM);
        Volume volume = new Volume(1000.0, VolumeUnit.CUBIC_CM);

        // when
        Sku sku = Sku.create(
            CreateSkuCommand.builder()
                .id(id)
                .code(code)
                .name(name)
                .description(description)
                .weight(weight)
                .volume(volume)
                .build(),
            FIXED_TIME
        );

        // then
        assertThat(sku.getId()).isEqualTo(id);
        assertThat(sku.getCode()).isEqualTo(code);
        assertThat(sku.getName()).isEqualTo(name);
        assertThat(sku.getDescription()).isEqualTo(description);
        assertThat(sku.getWeight()).isEqualTo(weight);
        assertThat(sku.getVolume()).isEqualTo(volume);
        assertThat(sku.getCreatedAt()).isNotNull();
        assertThat(sku.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("필수 값만으로 SKU를 생성할 수 있다")
    void shouldCreateSkuWithRequiredValuesOnly() {
        // given
        SkuId id = SkuId.generate();
        SkuCode code = new SkuCode("TSHIRT-BLACK-L");
        String name = "티셔츠 - 블랙 - L";

        // when
        Sku sku = Sku.create(
            CreateSkuCommand.builder()
                .id(id)
                .code(code)
                .name(name)
                .build(),
            FIXED_TIME
        );

        // then
        assertThat(sku.getId()).isEqualTo(id);
        assertThat(sku.getCode()).isEqualTo(code);
        assertThat(sku.getName()).isEqualTo(name);
        assertThat(sku.getDescription()).isNull();
        assertThat(sku.getWeight()).isNull();
        assertThat(sku.getVolume()).isNull();
    }

    @Test
    @DisplayName("ID가 없으면 SKU 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenIdIsNull() {
        // given
        SkuCode code = new SkuCode("TSHIRT-BLACK-L");
        String name = "티셔츠 - 블랙 - L";

        // when & then
        assertThatThrownBy(() -> Sku.create(
            CreateSkuCommand.builder()
                .code(code)
                .name(name)
                .build(),
            FIXED_TIME
        ))
            .isInstanceOf(InvalidSkuException.class)
            .hasMessage("SKU ID는 필수입니다");
    }

    @Test
    @DisplayName("코드가 없으면 SKU 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenCodeIsNull() {
        // given
        SkuId id = SkuId.generate();
        String name = "티셔츠 - 블랙 - L";

        // when & then
        assertThatThrownBy(() -> Sku.create(
            CreateSkuCommand.builder()
                .id(id)
                .name(name)
                .build(),
            FIXED_TIME
        ))
            .isInstanceOf(InvalidSkuException.class)
            .hasMessage("SKU 코드는 필수입니다");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("이름이 없거나 빈 값이면 SKU 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenNameIsNullOrEmpty(String invalidName) {
        // given
        SkuId id = SkuId.generate();
        SkuCode code = new SkuCode("TSHIRT-BLACK-L");

        // when & then
        assertThatThrownBy(() -> Sku.create(
            CreateSkuCommand.builder()
                .id(id)
                .code(code)
                .name(invalidName)
                .build(),
            FIXED_TIME
        ))
            .isInstanceOf(InvalidSkuException.class)
            .hasMessage("SKU 이름은 필수입니다");
    }

    @Test
    @DisplayName("SKU 정보를 업데이트할 수 있다")
    void shouldUpdateSku() {
        // given
        Sku sku = createDefaultSku();
        String newName = "업데이트된 티셔츠";
        String newDescription = "업데이트된 설명";
        Weight newWeight = new Weight(300.0, WeightUnit.GRAM);
        Volume newVolume = new Volume(1200.0, VolumeUnit.CUBIC_CM);
        LocalDateTime beforeUpdate = sku.getUpdatedAt();
        LocalDateTime updateTime = FIXED_TIME.plusHours(1);

        // when
        sku.update(
            UpdateSkuCommand.builder()
                .name(newName)
                .description(newDescription)
                .weight(newWeight)
                .volume(newVolume)
                .build(),
            updateTime
        );

        // then
        assertThat(sku.getName()).isEqualTo(newName);
        assertThat(sku.getDescription()).isEqualTo(newDescription);
        assertThat(sku.getWeight()).isEqualTo(newWeight);
        assertThat(sku.getVolume()).isEqualTo(newVolume);
        assertThat(sku.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("SKU 업데이트 시 null 값은 무시된다")
    void shouldIgnoreNullValuesWhenUpdate() {
        // given
        Sku sku = createDefaultSku();
        String originalName = sku.getName();
        String originalDescription = sku.getDescription();
        Weight originalWeight = sku.getWeight();
        Volume originalVolume = sku.getVolume();

        // when
        sku.update(UpdateSkuCommand.builder().build(), FIXED_TIME);

        // then
        assertThat(sku.getName()).isEqualTo(originalName);
        assertThat(sku.getDescription()).isEqualTo(originalDescription);
        assertThat(sku.getWeight()).isEqualTo(originalWeight);
        assertThat(sku.getVolume()).isEqualTo(originalVolume);
    }

    @Test
    @DisplayName("SKU 업데이트 시 빈 이름으로는 업데이트할 수 없다")
    void shouldThrowExceptionWhenUpdateWithEmptyName() {
        // given
        Sku sku = createDefaultSku();

        // when & then
        assertThatThrownBy(() -> sku.update(
            UpdateSkuCommand.builder()
                .name("")
                .build(),
            FIXED_TIME
        ))
            .isInstanceOf(InvalidSkuException.class)
            .hasMessage("SKU 이름은 필수입니다");
    }

    private Sku createDefaultSku() {
        return Sku.create(
            CreateSkuCommand.builder()
                .id(SkuId.generate())
                .code(new SkuCode("TSHIRT-BLACK-L"))
                .name("티셔츠 - 블랙 - L")
                .description("블랙 색상 티셔츠 L 사이즈")
                .weight(new Weight(250.0, WeightUnit.GRAM))
                .volume(new Volume(1000.0, VolumeUnit.CUBIC_CM))
                .build(),
            FIXED_TIME
        );
    }
}