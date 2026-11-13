package org.sopt.korail.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.reservation.domain.Ticket;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.seat.domain.Direction;
import org.sopt.korail.seat.domain.Seat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ReservationDetailResponse(
        @Schema(description = "예약 ID", example = "1")
        Long reservationId,

        @Schema(description = "예약 상태 (결제 대기 시간이 지난 PENDING은 EXPIRED)", example = "PENDING")
        ReservationStatus status,

        @Schema(description = "총 결제 금액", example = "119600")
        int totalPrice,

        @Schema(description = "결제 만료 시각", example = "2026-12-01T09:10:00")
        LocalDateTime expiresAt,

        @Schema(description = "예약 시각", example = "2026-12-01T09:00:00")
        LocalDateTime createdAt,

        @Schema(description = "열차 일정")
        ScheduleResponse schedule,

        @Schema(description = "승차권 목록")
        List<TicketResponse> tickets
) {
    public record ScheduleResponse(
            @Schema(description = "스케줄 ID", example = "1")
            Long scheduleId,

            @Schema(description = "열차 번호", example = "KTX001")
            String trainNumber,

            @Schema(description = "열차 종류", example = "ITX")
            TrainType trainType,

            @Schema(description = "열차 종류 표시 이름", example = "ITX-새마을")
            String trainTypeName,

            @Schema(description = "출발역", example = "서울")
            String departureStationName,

            @Schema(description = "도착역", example = "부산")
            String arrivalStationName,

            @Schema(description = "출발 시각", example = "2026-12-01T10:00:00")
            LocalDateTime departureAt,

            @Schema(description = "도착 시각", example = "2026-12-01T12:40:00")
            LocalDateTime arrivalAt,

            @Schema(description = "소요 시간(분)", example = "160")
            long durationMinutes,

            @Schema(description = "비고", example = "")
            String remark
    ) {
        public static ScheduleResponse of(Schedule schedule) {
            return new ScheduleResponse(
                    schedule.getId(),
                    schedule.getTrain().getTrainNumber(),
                    schedule.getTrain().getTrainType(),
                    schedule.getTrain().getTrainType().getDisplayName(),
                    schedule.getDepartureStation().getName(),
                    schedule.getArrivalStation().getName(),
                    schedule.getDepartureAt(),
                    schedule.getArrivalAt(),
                    Duration.between(schedule.getDepartureAt(), schedule.getArrivalAt()).toMinutes(),
                    schedule.getRemark()
            );
        }
    }

    public record TicketResponse(
            @Schema(description = "승차권 ID", example = "1")
            Long ticketId,

            @Schema(description = "호차 번호", example = "3")
            int coachNumber,

            @Schema(description = "좌석 등급", example = "STANDARD")
            SeatClass seatClass,

            @Schema(description = "좌석 ID", example = "101")
            Long seatId,

            @Schema(description = "좌석 번호", example = "1A")
            String seatNumber,

            @Schema(description = "좌석 방향", example = "FORWARD")
            Direction direction,

            @Schema(description = "승객 유형", example = "ADULT")
            PassengerType passengerType,

            @Schema(description = "승차권 금액", example = "59800")
            int price
    ) {
        public static TicketResponse of(Ticket ticket) {
            Seat seat = ticket.getSeat();
            return new TicketResponse(
                    ticket.getId(),
                    seat.getCoach().getCoachNumber(),
                    seat.getCoach().getSeatClass(),
                    seat.getId(),
                    seat.getSeatNumber(),
                    seat.getDirection(),
                    ticket.getPassengerType(),
                    ticket.getPrice()
            );
        }
    }
}
