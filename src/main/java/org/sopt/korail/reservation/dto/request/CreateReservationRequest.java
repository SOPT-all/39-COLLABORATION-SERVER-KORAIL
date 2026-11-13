package org.sopt.korail.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.sopt.korail.global.enums.SeatClass;

import java.util.List;

public record CreateReservationRequest(
        @Schema(description = "스케줄 ID", example = "1")
        @NotNull
        Long scheduleId,

        @Schema(description = "좌석 등급 (생략하면 STANDARD)", example = "STANDARD", defaultValue = "STANDARD")
        SeatClass seatClass,

        @Schema(description = "인원 수 (1~9)", example = "2")
        @Min(1) @Max(9)
        int passengerCount,

        @Schema(description = "선택한 좌석 ID 목록 (생략하면 자동 배정, 입력하면 인원 수와 같아야 함)", example = "[101, 102]")
        List<@NotNull Long> seatIds
) {
    public CreateReservationRequest {
        if (seatClass == null) {
            seatClass = SeatClass.STANDARD;
        }
    }
}
