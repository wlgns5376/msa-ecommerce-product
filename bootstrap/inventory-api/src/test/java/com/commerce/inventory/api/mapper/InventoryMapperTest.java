package com.commerce.inventory.api.mapper;

import com.commerce.inventory.api.dto.CreateSkuRequest;
import com.commerce.inventory.api.dto.CreateSkuResponseDto;
import com.commerce.inventory.application.usecase.CreateSkuCommand;
import com.commerce.inventory.application.usecase.CreateSkuResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryMapper 테스트")
class InventoryMapperTest {

    private InventoryMapper inventoryMapper;

    @BeforeEach
    void setUp() {
        inventoryMapper = new InventoryMapper();
    }

    @Nested
    @DisplayName("toCreateSkuCommand 메서드")
    class ToCreateSkuCommand {

        @Test
        @DisplayName("유효한 CreateSkuRequest를 CreateSkuCommand로 변환한다")
        void shouldConvertCreateSkuRequestToCommand() {
            // Given
            CreateSkuRequest request = CreateSkuRequest.builder()
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit("KG")
                    .volume(100.0)
                    .volumeUnit("LITER")
                    .build();

            // When
            CreateSkuCommand command = inventoryMapper.toCreateSkuCommand(request);

            // Then
            assertThat(command).isNotNull();
            assertThat(command.getCode()).isEqualTo("SKU001");
            assertThat(command.getName()).isEqualTo("테스트 상품");
            assertThat(command.getDescription()).isEqualTo("테스트 상품 설명");
            assertThat(command.getWeight()).isEqualTo(1.5);
            assertThat(command.getWeightUnit()).isEqualTo("KG");
            assertThat(command.getVolume()).isEqualTo(100.0);
            assertThat(command.getVolumeUnit()).isEqualTo("LITER");
        }

        @Test
        @DisplayName("선택적 필드가 없는 CreateSkuRequest를 CreateSkuCommand로 변환한다")
        void shouldConvertRequestWithoutOptionalFields() {
            // Given
            CreateSkuRequest request = CreateSkuRequest.builder()
                    .code("SKU002")
                    .name("최소 상품")
                    .build();

            // When
            CreateSkuCommand command = inventoryMapper.toCreateSkuCommand(request);

            // Then
            assertThat(command).isNotNull();
            assertThat(command.getCode()).isEqualTo("SKU002");
            assertThat(command.getName()).isEqualTo("최소 상품");
            assertThat(command.getDescription()).isNull();
            assertThat(command.getWeight()).isNull();
            assertThat(command.getWeightUnit()).isNull();
            assertThat(command.getVolume()).isNull();
            assertThat(command.getVolumeUnit()).isNull();
        }

        @Test
        @DisplayName("null CreateSkuRequest는 null을 반환한다")
        void shouldReturnNullForNullRequest() {
            // When
            CreateSkuCommand command = inventoryMapper.toCreateSkuCommand(null);

            // Then
            assertThat(command).isNull();
        }
    }

    @Nested
    @DisplayName("toCreateSkuResponseDto 메서드")
    class ToCreateSkuResponseDto {

        @Test
        @DisplayName("유효한 CreateSkuResponse를 CreateSkuResponseDto로 변환한다")
        void shouldConvertCreateSkuResponseToDto() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now();
            CreateSkuResponse response = CreateSkuResponse.builder()
                    .id("SKU-123456")
                    .code("SKU001")
                    .name("테스트 상품")
                    .description("테스트 상품 설명")
                    .weight(1.5)
                    .weightUnit("KG")
                    .volume(100.0)
                    .volumeUnit("LITER")
                    .createdAt(createdAt)
                    .build();

            // When
            CreateSkuResponseDto dto = inventoryMapper.toCreateSkuResponseDto(response);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo("SKU-123456");
            assertThat(dto.getCode()).isEqualTo("SKU001");
            assertThat(dto.getName()).isEqualTo("테스트 상품");
            assertThat(dto.getDescription()).isEqualTo("테스트 상품 설명");
            assertThat(dto.getWeight()).isEqualTo(1.5);
            assertThat(dto.getWeightUnit()).isEqualTo("KG");
            assertThat(dto.getVolume()).isEqualTo(100.0);
            assertThat(dto.getVolumeUnit()).isEqualTo("LITER");
            assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("선택적 필드가 없는 CreateSkuResponse를 CreateSkuResponseDto로 변환한다")
        void shouldConvertResponseWithoutOptionalFields() {
            // Given
            LocalDateTime createdAt = LocalDateTime.now();
            CreateSkuResponse response = CreateSkuResponse.builder()
                    .id("SKU-789012")
                    .code("SKU002")
                    .name("최소 상품")
                    .createdAt(createdAt)
                    .build();

            // When
            CreateSkuResponseDto dto = inventoryMapper.toCreateSkuResponseDto(response);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo("SKU-789012");
            assertThat(dto.getCode()).isEqualTo("SKU002");
            assertThat(dto.getName()).isEqualTo("최소 상품");
            assertThat(dto.getDescription()).isNull();
            assertThat(dto.getWeight()).isNull();
            assertThat(dto.getWeightUnit()).isNull();
            assertThat(dto.getVolume()).isNull();
            assertThat(dto.getVolumeUnit()).isNull();
            assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("null CreateSkuResponse는 null을 반환한다")
        void shouldReturnNullForNullResponse() {
            // When
            CreateSkuResponseDto dto = inventoryMapper.toCreateSkuResponseDto(null);

            // Then
            assertThat(dto).isNull();
        }
    }
}