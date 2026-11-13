package org.sopt.korail.seat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.reservation.repository.TicketRepository;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.domain.Train;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.sopt.korail.seat.code.SeatErrorCode;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Seat;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        Train train = schedule.getTrain();

        List<Coach> coaches = coachRepository.findCoachesByTrainAndSeatClass(train, seatClass);
        Map<Long, List<Seat>> seatsByCoachId = seatRepository.findSeatsByTrainAndSeatClass(train, seatClass)
                .stream()
                .collect(Collectors.groupingBy(seat -> seat.getCoach().getId()));
        Set<Long> occupiedSeatIds = findOccupiedSeatIds(scheduleId);

        List<CoachResponse> coachResponses = coaches.stream()
                .map(coach -> toCoachResponse(
                        coach, seatsByCoachId.getOrDefault(coach.getId(), List.of()), occupiedSeatIds))
                .toList();

        return CoachListResponse.of(scheduleId, seatClass, coachResponses);
    }

    public SeatLayoutResponse getSeats(Long scheduleId, Long coachId) {
        Schedule schedule = scheduleRepository.getByIdOrThrow(scheduleId);
        Coach coach = coachRepository.findByIdAndTrain(coachId, schedule.getTrain())
                .orElseThrow(() -> new GeneralException(SeatErrorCode.COACH_NOT_FOUND));

        List<Seat> seats = seatRepository.findSeatsByCoach(coach);
        Set<Long> occupiedSeatIds = findOccupiedSeatIds(scheduleId);

        List<SeatResponse> seatResponses = seats.stream()
                .map(seat -> SeatResponse.of(seat, !occupiedSeatIds.contains(seat.getId())))
                .toList();

        return SeatLayoutResponse.builder()
                .scheduleId(scheduleId)
                .coachId(coach.getId())
                .coachNumber(coach.getCoachNumber())
                .seatClass(coach.getSeatClass())
                .remainingSeatCount(countRemainingSeats(seats, occupiedSeatIds))
                .totalSeatCount(seats.size())
                .seats(seatResponses)
                .build();
    }

    private Set<Long> findOccupiedSeatIds(Long scheduleId) {
        return new HashSet<>(ticketRepository.findOccupiedSeatIds(scheduleId, LocalDateTime.now(clock)));
    }

    private int countRemainingSeats(List<Seat> seats, Set<Long> occupiedSeatIds) {
        return (int) seats.stream()
                .filter(seat -> !occupiedSeatIds.contains(seat.getId()))
                .count();
    }

    private CoachResponse toCoachResponse(Coach coach, List<Seat> seats, Set<Long> occupiedSeatIds) {
        int remainingSeatCount = countRemainingSeats(seats, occupiedSeatIds);

        return CoachResponse.builder()
                .coachId(coach.getId())
                .coachNumber(coach.getCoachNumber())
                .remainingSeatCount(remainingSeatCount)
                .totalSeatCount(seats.size())
                .reservable(remainingSeatCount > 0)
                .build();
    }
}
