package org.sopt.korail.reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.sopt.korail.global.enums.SeatClass;

import java.util.List;

public record CreateReservationRequest(
        @NotNull Long scheduleId,
        @NotNull SeatClass seatClass,
        @Min(1) @Max(9) int passengerCount,
        List<@NotNull Long> seatIds
) {
}
