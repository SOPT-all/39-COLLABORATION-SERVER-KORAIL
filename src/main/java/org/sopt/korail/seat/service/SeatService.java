package org.sopt.korail.seat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.sopt.korail.seat.dto.CoachSeatSummary;
import org.sopt.korail.seat.dto.response.CoachListResponse;
import org.sopt.korail.seat.dto.response.CoachResponse;
import org.sopt.korail.seat.repository.CoachRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {
    private final ScheduleRepository scheduleRepository;
    private final CoachRepository coachRepository;

    public CoachListResponse getCoaches(Long scheduleId, SeatClass seatClass) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        List<CoachResponse> coaches = coachRepository.findCoachSeatSummaries(schedule, seatClass, LocalDateTime.now())
                .stream()
                .map(this::toCoachResponse)
                .toList();

        return CoachListResponse.of(scheduleId, seatClass, coaches);
    }

    private CoachResponse toCoachResponse(CoachSeatSummary summary) {
        int totalSeatCount = (int) summary.totalSeatCount();
        int remainingSeatCount = totalSeatCount - (int) summary.occupiedSeatCount();

        return CoachResponse.builder()
                .coachId(summary.coachId())
                .coachNumber(summary.coachNumber())
                .remainingSeatCount(remainingSeatCount)
                .totalSeatCount(totalSeatCount)
                .reservable(remainingSeatCount > 0)
                .build();
    }
}
