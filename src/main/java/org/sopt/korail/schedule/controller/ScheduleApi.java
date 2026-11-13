package org.sopt.korail.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.dto.response.ScheduleDetailResponse;
import org.sopt.korail.schedule.dto.response.ScheduleListResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "열차 API", description = "열차 조회와 관련된 API 입니다")
@RequestMapping(path = "/api/v1/schedules")
public interface ScheduleApi {
    @Operation(summary = "열차 목록 조회", description = "서울-부산 구간의 열차 목록을 열차 종류별 운임과 함께 조회합니다. 출발일과 열차 종류는 선택 조건입니다.")
    @SecurityRequirements
    @GetMapping
    ScheduleListResponse getSchedules(
            @Parameter(description = "출발일 (yyyy-MM-dd), 미입력 시 전체", example = "2026-12-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @Parameter(description = "인원수 (1~9, 기본값 1)", example = "1")
            @RequestParam(defaultValue = "1") int passengerCount,
            @Parameter(description = "열차 종류 필터 (쉼표 구분), 미입력 시 전체", example = "KTX,ITX")
            @RequestParam(required = false) List<TrainType> trainTypes
    );

    @Operation(summary = "열차 상세 및 운임 조회", description = "선택한 열차의 상세 정보와 좌석 등급별 운임을 인원수를 반영해 조회합니다.")
    @SecurityRequirements
    @GetMapping("/{scheduleId}")
    ScheduleDetailResponse getSchedule(
            @Parameter(description = "스케줄 ID", example = "1")
            @PathVariable Long scheduleId,
            @Parameter(description = "인원수 (1~9, 기본값 1)", example = "1")
            @RequestParam(defaultValue = "1") int passengerCount
    );
}
