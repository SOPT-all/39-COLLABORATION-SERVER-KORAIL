package org.sopt.korail.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.member.code.ApiMemberErrors;
import org.sopt.korail.member.code.MemberErrorCode;
import org.sopt.korail.reservation.code.ApiReservationErrors;
import org.sopt.korail.reservation.code.ApiReservationSuccess;
import org.sopt.korail.reservation.code.ReservationErrorCode;
import org.sopt.korail.reservation.code.ReservationSuccessCode;
import org.sopt.korail.reservation.dto.request.CreateReservationRequest;
import org.sopt.korail.reservation.dto.response.CreateReservationResponse;
import org.sopt.korail.reservation.dto.response.ReservationDetailResponse;
import org.sopt.korail.reservation.service.ReservationService;
import org.sopt.korail.schedule.code.ApiScheduleErrors;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.seat.code.ApiSeatErrors;
import org.sopt.korail.seat.code.SeatErrorCode;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReservationController implements ReservationApi {

    private final ReservationService reservationService;

    @Override
    @ApiReservationSuccess(ReservationSuccessCode.RESERVATION_CREATED)
    @ApiReservationErrors({
            ReservationErrorCode.SEAT_COUNT_MISMATCH,
            ReservationErrorCode.SEAT_NOT_IN_SCHEDULE,
            ReservationErrorCode.SEAT_CLASS_MISMATCH,
            ReservationErrorCode.SEAT_ALREADY_RESERVED,
            ReservationErrorCode.INSUFFICIENT_SEATS
    })
    @ApiScheduleErrors({ScheduleErrorCode.SCHEDULE_NOT_FOUND, ScheduleErrorCode.FARE_NOT_FOUND})
    @ApiSeatErrors({SeatErrorCode.SEAT_NOT_FOUND})
    @ApiMemberErrors({MemberErrorCode.MEMBER_NOT_FOUND})
    public CreateReservationResponse createReservation(Long memberId, CreateReservationRequest request) {
        return reservationService.createReservation(memberId, request);
    }

    @Override
    @ApiReservationSuccess(ReservationSuccessCode.RESERVATION_DETAIL_OK)
    @ApiReservationErrors({ReservationErrorCode.RESERVATION_NOT_FOUND})
    public ReservationDetailResponse getReservation(Long memberId, Long reservationId) {
        return reservationService.getReservation(memberId, reservationId);
    }
}
