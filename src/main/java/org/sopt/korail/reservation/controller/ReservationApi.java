package org.sopt.korail.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sopt.korail.reservation.dto.request.CreateReservationRequest;
import org.sopt.korail.reservation.dto.response.CreateReservationResponse;
import org.sopt.korail.reservation.dto.response.ReservationDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "예약 API", description = "예약과 관련된 API 입니다")
@RequestMapping(path = "/api/v1/reservations")
public interface ReservationApi {
    @Operation(summary = "예약 생성", description = "선택한 일정의 좌석을 예약합니다. 좌석을 선택하지 않으면 자동으로 배정합니다.")
    @SecurityRequirements
    @PostMapping
    CreateReservationResponse createReservation(
            @Valid @RequestBody CreateReservationRequest request
    );

    @Operation(summary = "예약 상세 조회", description = "예약한 일정과 좌석 정보를 조회합니다.")
    @SecurityRequirements
    @GetMapping("/{reservationId}")
    ReservationDetailResponse getReservation(
            @Parameter(description = "예약 ID", example = "1")
            @PathVariable Long reservationId
    );
}
