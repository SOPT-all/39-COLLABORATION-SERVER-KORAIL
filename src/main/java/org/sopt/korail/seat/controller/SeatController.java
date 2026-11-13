package org.sopt.korail.seat.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.schedule.code.ApiScheduleErrors;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.seat.code.ApiSeatErrors;
import org.sopt.korail.seat.code.ApiSeatSuccess;
import org.sopt.korail.seat.code.SeatErrorCode;
import org.sopt.korail.seat.code.SeatSuccessCode;
import org.sopt.korail.seat.dto.response.CoachListResponse;
import org.sopt.korail.seat.dto.response.SeatLayoutResponse;
import org.sopt.korail.seat.service.SeatService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeatController implements SeatApi {
    private final SeatService seatService;

    @Override
    @ApiSeatSuccess(SeatSuccessCode.COACH_LIST_OK)
    @ApiScheduleErrors({ScheduleErrorCode.SCHEDULE_NOT_FOUND})
    public CoachListResponse getCoaches(Long scheduleId, SeatClass seatClass) {
        return seatService.getCoaches(scheduleId, seatClass);
    }

    @Override
    @ApiSeatSuccess(SeatSuccessCode.SEAT_LAYOUT_OK)
    @ApiScheduleErrors({ScheduleErrorCode.SCHEDULE_NOT_FOUND})
    @ApiSeatErrors({SeatErrorCode.COACH_NOT_FOUND})
    public SeatLayoutResponse getSeats(Long scheduleId, Long coachId) {
        return seatService.getSeats(scheduleId, coachId);
    }
}
