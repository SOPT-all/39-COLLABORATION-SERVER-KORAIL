package org.sopt.korail.seat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.korail.global.enums.SeatClass;

import java.util.List;

public record CoachListResponse(
        @Schema(description = "스케줄 ID", example = "1")
        Long scheduleId,

        @Schema(description = "좌석 등급", example = "STANDARD")
        SeatClass seatClass,

        @Schema(description = "호차 목록 (호차 번호 오름차순)")
        List<CoachResponse> coaches
) {
    public static CoachListResponse of(Long scheduleId, SeatClass seatClass, List<CoachResponse> coaches) {
        return new CoachListResponse(scheduleId, seatClass, coaches);
    }
}
