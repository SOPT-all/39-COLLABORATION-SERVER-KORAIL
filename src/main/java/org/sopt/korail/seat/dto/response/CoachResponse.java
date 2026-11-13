package org.sopt.korail.seat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CoachResponse(
        @Schema(description = "호차 ID", example = "1")
        Long coachId,

        @Schema(description = "호차 번호", example = "1")
        int coachNumber,

        @Schema(description = "잔여 좌석 수", example = "17")
        int remainingSeatCount,

        @Schema(description = "전체 좌석 수", example = "48")
        int totalSeatCount,

        @Schema(description = "좌석 선택 가능 여부", example = "true")
        boolean reservable
) {
}
