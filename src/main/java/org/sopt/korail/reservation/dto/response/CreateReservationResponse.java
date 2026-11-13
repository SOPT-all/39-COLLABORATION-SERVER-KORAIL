package org.sopt.korail.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.korail.reservation.domain.Reservation;

public record CreateReservationResponse(
        @Schema(description = "예약 ID", example = "1")
        Long reservationId
) {
    public static CreateReservationResponse of(Reservation reservation) {
        return new CreateReservationResponse(reservation.getId());
    }
}
