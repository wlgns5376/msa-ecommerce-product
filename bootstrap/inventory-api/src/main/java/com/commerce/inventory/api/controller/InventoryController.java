package com.commerce.inventory.api.controller;

import com.commerce.inventory.api.dto.CreateSkuRequest;
import com.commerce.inventory.api.dto.CreateSkuResponseDto;
import com.commerce.inventory.application.usecase.CreateSkuCommand;
import com.commerce.inventory.application.usecase.CreateSkuResponse;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 관리 REST API 컨트롤러
 */
@Tag(name = "Inventory", description = "재고 관리 API")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final CreateSkuUseCase createSkuUseCase;

    /**
     * SKU 생성 엔드포인트
     *
     * @param request SKU 생성 요청 정보
     * @return 생성된 SKU 정보
     */
    @Operation(summary = "SKU 생성", description = "새로운 SKU(Stock Keeping Unit)를 생성합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "SKU 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "409", description = "중복된 SKU 코드"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/skus")
    public ResponseEntity<CreateSkuResponseDto> createSku(@Valid @RequestBody CreateSkuRequest request) {
        CreateSkuCommand command = CreateSkuCommand.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit())
                .volume(request.getVolume())
                .volumeUnit(request.getVolumeUnit())
                .build();

        CreateSkuResponse response = createSkuUseCase.execute(command);

        CreateSkuResponseDto responseDto = CreateSkuResponseDto.builder()
                .id(response.getId())
                .code(response.getCode())
                .name(response.getName())
                .description(response.getDescription())
                .weight(response.getWeight())
                .weightUnit(response.getWeightUnit())
                .volume(response.getVolume())
                .volumeUnit(response.getVolumeUnit())
                .createdAt(response.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
}