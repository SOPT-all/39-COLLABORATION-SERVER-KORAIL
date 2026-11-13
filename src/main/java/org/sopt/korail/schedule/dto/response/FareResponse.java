package org.sopt.korail.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.korail.global.enums.SeatClass;

public record FareResponse(
        @Schema(description = "좌석 등급", example = "STANDARD")
        SeatClass seatClass,

        @Schema(description = "1인당 운임", example = "78400")
        int unitPrice,

        @Schema(description = "인원수를 반영한 총 운임", example = "78400")
        int totalPrice
) {
    public static FareResponse of(SeatClass seatClass, int unitPrice, int passengerCount) {
        return new FareResponse(seatClass, unitPrice, unitPrice * passengerCount);
    }
}
