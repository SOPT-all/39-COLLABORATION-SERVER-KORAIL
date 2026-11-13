package org.sopt.korail.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ScheduleListResponse(
        @Schema(description = "조회에 적용한 인원수", example = "1")
        int passengerCount,

        @Schema(description = "열차 목록 (출발 시각 오름차순, 결과가 없으면 빈 배열)")
        List<ScheduleResponse> schedules
) {
    public static ScheduleListResponse of(int passengerCount, List<ScheduleResponse> schedules) {
        return new ScheduleListResponse(passengerCount, schedules);
    }
}
