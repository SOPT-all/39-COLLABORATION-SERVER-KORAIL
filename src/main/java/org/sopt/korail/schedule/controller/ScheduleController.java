package org.sopt.korail.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.CommonErrorCode;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.global.swagger.ApiCommonErrors;
import org.sopt.korail.schedule.code.ApiScheduleErrors;
import org.sopt.korail.schedule.code.ApiScheduleSuccess;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.schedule.code.ScheduleSuccessCode;
import org.sopt.korail.schedule.dto.response.ScheduleDetailResponse;
import org.sopt.korail.schedule.dto.response.ScheduleListResponse;
import org.sopt.korail.schedule.service.ScheduleService;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApi {
    private final ScheduleService scheduleService;

    @Override
    @ApiScheduleSuccess(ScheduleSuccessCode.SCHEDULE_LIST_OK)
    @ApiCommonErrors({CommonErrorCode.BAD_REQUEST})
    public ScheduleListResponse getSchedules(LocalDate departureDate, int passengerCount, List<TrainType> trainTypes) {
        return scheduleService.getSchedules(departureDate, passengerCount, trainTypes);
    }

    @Override
    @ApiScheduleSuccess(ScheduleSuccessCode.SCHEDULE_DETAIL_OK)
    @ApiCommonErrors({CommonErrorCode.BAD_REQUEST})
    @ApiScheduleErrors({ScheduleErrorCode.SCHEDULE_NOT_FOUND})
    public ScheduleDetailResponse getSchedule(Long scheduleId, int passengerCount) {
        return scheduleService.getSchedule(scheduleId, passengerCount);
    }
}
