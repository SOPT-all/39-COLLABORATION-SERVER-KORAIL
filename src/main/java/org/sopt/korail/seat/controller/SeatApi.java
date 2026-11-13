package org.sopt.korail.seat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.seat.dto.response.CoachListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "좌석 API", description = "좌석과 관련된 API 입니다")
@RequestMapping(path = "/api/v1/schedules/{scheduleId}/coaches")
public interface SeatApi {
    @Operation(summary = "호차 목록 조회", description = "선택한 열차와 좌석 등급의 호차 목록을 호차별 잔여 좌석 수와 함께 조회합니다.")
    @SecurityRequirements
    @GetMapping
    CoachListResponse getCoaches(
            @Parameter(description = "스케줄 ID", example = "1")
            @PathVariable Long scheduleId,
            @Parameter(description = "좌석 등급 (기본값 STANDARD)", example = "STANDARD")
            @RequestParam(defaultValue = "STANDARD") SeatClass seatClass
    );
}
