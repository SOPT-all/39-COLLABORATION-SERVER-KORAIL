package org.sopt.korail.seat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.reservation.repository.TicketRepository;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.sopt.korail.seat.code.SeatErrorCode;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Seat;
import org.sopt.korail.seat.dto.CoachSeatSummary;
import org.sopt.korail.seat.dto.response.CoachListResponse;
import org.sopt.korail.seat.dto.response.CoachResponse;
import org.sopt.korail.seat.dto.response.SeatLayoutResponse;
import org.sopt.korail.seat.dto.response.SeatResponse;
import org.sopt.korail.seat.repository.CoachRepository;
import org.sopt.korail.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {
    private final ScheduleRepository scheduleRepository;
    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final Clock clock;

    public CoachListResponse getCoaches(Long scheduleId, SeatClass seatClass) {
        Schedule schedule = scheduleRepository.getByIdOrThrow(scheduleId);

        List<CoachResponse> coaches = coachRepository.findCoachSeatSummaries(schedule, seatClass, LocalDateTime.now(clock))
                .stream()
                .map(this::toCoachResponse)
                .toList();

        return CoachListResponse.of(scheduleId, seatClass, coaches);
    }

    public SeatLayoutResponse getSeats(Long scheduleId, Long coachId) {
        Schedule schedule = scheduleRepository.getByIdOrThrow(scheduleId);
        Coach coach = coachRepository.findByIdAndTrain(coachId, schedule.getTrain())
                .orElseThrow(() -> new GeneralException(SeatErrorCode.COACH_NOT_FOUND));

        List<Seat> seats = seatRepository.findSeats(coach);
        Set<Long> occupiedSeatIds = new HashSet<>(
                ticketRepository.findOccupiedSeatIds(schedule, coach, LocalDateTime.now(clock)));

        List<SeatResponse> seatResponses = seats.stream()
                .map(seat -> SeatResponse.of(seat, !occupiedSeatIds.contains(seat.getId())))
                .toList();

        return SeatLayoutResponse.builder()
                .scheduleId(scheduleId)
                .coachId(coach.getId())
                .coachNumber(coach.getCoachNumber())
                .seatClass(coach.getSeatClass())
                .remainingSeatCount(seats.size() - occupiedSeatIds.size())
                .totalSeatCount(seats.size())
                .seats(seatResponses)
                .build();
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
