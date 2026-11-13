package org.sopt.korail.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Schedule;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record ScheduleResponse(
        @Schema(description = "일정 ID (호차·좌석 조회의 scheduleId와 같은 값)", example = "1")
        Long scheduleId,

        @Schema(description = "열차 번호", example = "001")
        String trainNumber,

        @Schema(description = "열차 종류", example = "ITX")
        TrainType trainType,

        @Schema(description = "열차 종류 표시 이름", example = "KTX")
        String trainTypeName,

        @Schema(description = "출발 시각", example = "2026-11-20T05:13:00")
        LocalDateTime departureAt,

        @Schema(description = "도착 시각", example = "2026-11-20T07:50:00")
        LocalDateTime arrivalAt,

        @Schema(description = "소요 시간(분)", example = "157")
        int durationMinutes,

        @Schema(description = "특이사항 (정차역 표시 등, 없으면 null)", example = "대전·동대구 정차", nullable = true)
        String remark,

        @Schema(description = "좌석 등급별 운임 (일반실, 특실)")
        List<FareResponse> fares
) {
    public static ScheduleResponse of(Schedule schedule, List<FareResponse> fares) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTrain().getTrainNumber(),
                schedule.getTrain().getTrainType(),
                schedule.getTrain().getTrainType().getDisplayName(),
                schedule.getDepartureAt(),
                schedule.getArrivalAt(),
                (int) Duration.between(schedule.getDepartureAt(), schedule.getArrivalAt()).toMinutes(),
                schedule.getRemark(),
                fares
        );
    }
}
