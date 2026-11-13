package org.sopt.korail.reservation.dto;

import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.seat.domain.Direction;

import java.time.LocalDateTime;
import java.util.List;

public record ReservationDetailResponse(
        Long reservationId,
        ReservationStatus status,
        int totalPrice,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        ScheduleResponse schedule,
        List<TicketResponse> tickets
) {
    public record ScheduleResponse(
            Long scheduleId,
            String trainNumber,
            TrainType trainType,
            String trainTypeName,
            String departureStationName,
            String arrivalStationName,
            LocalDateTime departureAt,
            LocalDateTime arrivalAt,
            long durationMinutes,
            String remark
    ) {
    }

    public record TicketResponse(
            Long ticketId,
            int coachNumber,
            SeatClass seatClass,
            Long seatId,
            String seatNumber,
            Direction direction,
            PassengerType passengerType,
            int price
    ) {
    }
}
