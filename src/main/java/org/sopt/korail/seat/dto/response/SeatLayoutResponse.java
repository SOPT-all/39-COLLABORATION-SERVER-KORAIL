package org.sopt.korail.seat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.sopt.korail.global.enums.SeatClass;

import java.util.List;

@Builder
public record SeatLayoutResponse(
        @Schema(description = "스케줄 ID", example = "1")
        Long scheduleId,

        @Schema(description = "호차 ID", example = "1")
        Long coachId,

        @Schema(description = "호차 번호", example = "1")
        int coachNumber,

        @Schema(description = "좌석 등급", example = "STANDARD")
        SeatClass seatClass,

        @Schema(description = "잔여 좌석 수", example = "17")
        int remainingSeatCount,

        @Schema(description = "전체 좌석 수", example = "48")
        int totalSeatCount,

        @Schema(description = "좌석 목록 (행 오름차순, 열 오름차순)")
        List<SeatResponse> seats
) {
}
