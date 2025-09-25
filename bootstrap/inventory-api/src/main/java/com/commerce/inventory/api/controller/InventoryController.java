package com.commerce.inventory.api.controller;

import com.commerce.inventory.api.dto.CreateSkuRequest;
import com.commerce.inventory.api.dto.CreateSkuResponseDto;
import com.commerce.inventory.api.dto.GetSkuByIdResponseDto;
import com.commerce.inventory.api.dto.ReceiveStockRequest;
import com.commerce.inventory.api.dto.ReserveStockRequest;
import com.commerce.inventory.api.dto.ReserveStockResponseDto;
import com.commerce.inventory.api.mapper.InventoryMapper;
import com.commerce.inventory.application.usecase.CreateSkuCommand;
import com.commerce.inventory.application.usecase.CreateSkuResponse;
import com.commerce.inventory.application.usecase.CreateSkuUseCase;
import com.commerce.inventory.application.usecase.GetSkuByIdQuery;
import com.commerce.inventory.application.usecase.GetSkuByIdResponse;
import com.commerce.inventory.application.usecase.GetSkuByIdUseCase;
import com.commerce.inventory.application.usecase.ReceiveStockCommand;
import com.commerce.inventory.application.usecase.ReceiveStockUseCase;
import com.commerce.inventory.application.usecase.ReleaseReservationCommand;
import com.commerce.inventory.application.usecase.ReleaseReservationUseCase;
import com.commerce.inventory.application.usecase.ReserveStockCommand;
import com.commerce.inventory.application.usecase.ReserveStockResponse;
import com.commerce.inventory.application.usecase.ReserveStockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final GetSkuByIdUseCase getSkuByIdUseCase;
    private final ReceiveStockUseCase receiveStockUseCase;
    private final ReserveStockUseCase reserveStockUseCase;
    private final ReleaseReservationUseCase releaseReservationUseCase;
    private final InventoryMapper inventoryMapper;

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
        CreateSkuCommand command = inventoryMapper.toCreateSkuCommand(request);
        CreateSkuResponse response = createSkuUseCase.execute(command);
        CreateSkuResponseDto responseDto = inventoryMapper.toCreateSkuResponseDto(response);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * SKU 조회 엔드포인트
     *
     * @param id 조회할 SKU ID
     * @return SKU 정보
     */
    @Operation(summary = "SKU 조회", description = "ID로 SKU(Stock Keeping Unit)를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 조회 성공"),
            @ApiResponse(responseCode = "404", description = "SKU를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/skus/{id}")
    public ResponseEntity<GetSkuByIdResponseDto> getSkuById(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable("id") String id) {
        GetSkuByIdQuery query = GetSkuByIdQuery.of(id);
        GetSkuByIdResponse response = getSkuByIdUseCase.execute(query);
        GetSkuByIdResponseDto responseDto = inventoryMapper.toGetSkuByIdResponseDto(response);
        
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 재고 입고 엔드포인트
     *
     * @param id 입고할 SKU ID
     * @param request 입고 정보 (수량, 참조번호)
     * @return HTTP 200 OK
     */
    @Operation(summary = "재고 입고", description = "특정 SKU에 재고를 입고합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재고 입고 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "404", description = "SKU를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/skus/{id}/receive")
    public ResponseEntity<Void> receiveStock(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable("id") String id,
            @Valid @RequestBody ReceiveStockRequest request) {
        
        ReceiveStockCommand command = ReceiveStockCommand.builder()
                .skuId(id)
                .quantity(request.getQuantity())
                .reference(request.getReference())
                .build();
        
        receiveStockUseCase.receive(command);
        
        return ResponseEntity.ok().build();
    }

    /**
     * 재고 예약 엔드포인트
     *
     * @param request 재고 예약 요청 정보
     * @return 예약 결과
     */
    @Operation(summary = "재고 예약", description = "지정된 SKU들의 재고를 예약합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "재고 예약 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "404", description = "SKU를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "재고 부족"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/reservations")
    public ResponseEntity<ReserveStockResponseDto> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        ReserveStockCommand command = inventoryMapper.toReserveStockCommand(request);
        ReserveStockResponse response = reserveStockUseCase.execute(command);
        ReserveStockResponseDto responseDto = inventoryMapper.toReserveStockResponseDto(response);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 재고 예약 취소 엔드포인트
     *
     * @param id 취소할 예약 ID
     * @return HTTP 204 No Content
     */
    @Operation(summary = "재고 예약 취소", description = "지정된 예약 ID의 재고 예약을 취소하고 재고를 해제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "재고 예약 취소 성공"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "예약 상태 충돌 (이미 취소됨, 만료됨 등)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> releaseReservation(
            @Parameter(description = "예약 ID", required = true)
            @PathVariable("id") String id) {
        
        ReleaseReservationCommand command = ReleaseReservationCommand.builder()
                .reservationId(id)
                .build();
        
        releaseReservationUseCase.release(command);
        
        return ResponseEntity.noContent().build();
    }
}