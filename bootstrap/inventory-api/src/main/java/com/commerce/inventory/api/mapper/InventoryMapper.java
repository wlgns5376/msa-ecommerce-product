package com.commerce.inventory.api.mapper;

import com.commerce.inventory.api.dto.CreateSkuRequest;
import com.commerce.inventory.api.dto.CreateSkuResponseDto;
import com.commerce.inventory.application.usecase.CreateSkuCommand;
import com.commerce.inventory.application.usecase.CreateSkuResponse;
import org.springframework.stereotype.Component;

/**
 * 재고 관련 DTO와 도메인 객체 간의 변환을 담당하는 매퍼
 * Controller와 UseCase 간의 데이터 변환 책임을 분리
 */
@Component
public class InventoryMapper {

    /**
     * CreateSkuRequest를 CreateSkuCommand로 변환
     *
     * @param request SKU 생성 요청 DTO
     * @return SKU 생성 커맨드
     */
    public CreateSkuCommand toCreateSkuCommand(CreateSkuRequest request) {
        if (request == null) {
            return null;
        }

        return CreateSkuCommand.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit())
                .volume(request.getVolume())
                .volumeUnit(request.getVolumeUnit())
                .build();
    }

    /**
     * CreateSkuResponse를 CreateSkuResponseDto로 변환
     *
     * @param response UseCase 응답
     * @return API 응답 DTO
     */
    public CreateSkuResponseDto toCreateSkuResponseDto(CreateSkuResponse response) {
        if (response == null) {
            return null;
        }

        return CreateSkuResponseDto.builder()
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
    }
}