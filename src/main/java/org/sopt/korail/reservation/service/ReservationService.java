package org.sopt.korail.reservation.service;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.member.code.MemberErrorCode;
import org.sopt.korail.member.domain.Member;
import org.sopt.korail.member.repository.MemberRepository;
import org.sopt.korail.reservation.code.ReservationErrorCode;
import org.sopt.korail.reservation.domain.Reservation;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.reservation.domain.Ticket;
import org.sopt.korail.reservation.dto.request.CreateReservationRequest;
import org.sopt.korail.reservation.dto.response.CreateReservationResponse;
import org.sopt.korail.reservation.dto.response.ReservationDetailResponse;
import org.sopt.korail.reservation.repository.ReservationRepository;
import org.sopt.korail.reservation.repository.TicketRepository;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.schedule.domain.Fare;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.repository.FareRepository;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.sopt.korail.seat.code.SeatErrorCode;
import org.sopt.korail.seat.domain.Seat;
import org.sopt.korail.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private static final int PAYMENT_TIMEOUT_MINUTES = 10;

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final ScheduleRepository scheduleRepository;
    private final FareRepository fareRepository;
    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Transactional
    public CreateReservationResponse createReservation(Long memberId, CreateReservationRequest request) {
        Member member = findMember(memberId);

        // 같은 일정의 좌석 점유 확인과 티켓 생성을 직렬화해 동시 중복 예약을 막는다.
        Schedule schedule = findScheduleForUpdate(request.scheduleId());
        LocalDateTime now = LocalDateTime.now(clock);
        Set<Long> occupiedSeatIds = new HashSet<>(
                ticketRepository.findOccupiedSeatIds(schedule.getId(), now)
        );

        List<Seat> seats = request.seatIds() == null
                ? assignSeats(schedule, request.seatClass(), request.passengerCount(), occupiedSeatIds)
                : validateAndGetSelectedSeats(schedule, request, occupiedSeatIds);

        Fare fare = findAdultFare(schedule, request.seatClass());

        int totalPrice = fare.getPrice() * request.passengerCount();

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .schedule(schedule)
                .member(member)
                .status(ReservationStatus.PENDING)
                .totalPrice(totalPrice)
                .expiresAt(now.plusMinutes(PAYMENT_TIMEOUT_MINUTES))
                .build());

        List<Ticket> tickets = seats.stream()
                .map(seat -> Ticket.builder()
                        .reservation(reservation)
                        .seat(seat)
                        .passengerType(PassengerType.ADULT)
                        .price(fare.getPrice())
                        .build())
                .toList();
        ticketRepository.saveAll(tickets);

        return CreateReservationResponse.of(reservation);
    }

    public ReservationDetailResponse getReservation(Long memberId, Long reservationId) {
        Reservation reservation = findReservation(memberId, reservationId);
        List<ReservationDetailResponse.TicketResponse> tickets =
                ticketRepository.findActiveTicketsByReservationId(reservationId).stream()
                        .map(ReservationDetailResponse.TicketResponse::of)
                        .toList();

        return ReservationDetailResponse.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatusAt(LocalDateTime.now(clock)))
                .totalPrice(reservation.getTotalPrice())
                .expiresAt(reservation.getExpiresAt())
                .createdAt(reservation.getCreatedAt())
                .schedule(ReservationDetailResponse.ScheduleResponse.of(reservation.getSchedule()))
                .tickets(tickets)
                .build();
    }

    private Schedule findScheduleForUpdate(Long scheduleId) {
        return scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    // 다른 회원의 예약은 존재 여부를 드러내지 않도록 없는 예약과 같게 처리한다.
    private Reservation findReservation(Long memberId, Long reservationId) {
        return reservationRepository.findDetailById(reservationId)
                .filter(reservation -> reservation.isOwnedBy(memberId))
                .orElseThrow(() -> new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    private Fare findAdultFare(Schedule schedule, SeatClass seatClass) {
        return fareRepository.findFare(
                        schedule.getDepartureStation().getId(),
                        schedule.getArrivalStation().getId(),
                        schedule.getTrain().getTrainType(),
                        seatClass,
                        PassengerType.ADULT)
                .orElseThrow(() -> new GeneralException(ScheduleErrorCode.FARE_NOT_FOUND));
    }

    private List<Seat> validateAndGetSelectedSeats(
            Schedule schedule,
            CreateReservationRequest request,
            Set<Long> occupiedSeatIds
    ) {
        List<Long> requestedIds = request.seatIds();
        if (requestedIds.size() != request.passengerCount()
                || new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new GeneralException(ReservationErrorCode.SEAT_COUNT_MISMATCH);
        }

        List<Seat> foundSeats = seatRepository.findAllByIdIn(requestedIds);
        if (foundSeats.size() != requestedIds.size()) {
            throw new GeneralException(SeatErrorCode.SEAT_NOT_FOUND);
        }
        Map<Long, Seat> seatsById = foundSeats.stream()
                .collect(Collectors.toMap(Seat::getId, Function.identity()));
        List<Seat> orderedSeats = requestedIds.stream().map(seatsById::get).toList();

        if (orderedSeats.stream().anyMatch(
                seat -> !seat.getCoach().getTrain().getId().equals(schedule.getTrain().getId()))) {
            throw new GeneralException(ReservationErrorCode.SEAT_NOT_IN_SCHEDULE);
        }
        if (orderedSeats.stream().anyMatch(
                seat -> seat.getCoach().getSeatClass() != request.seatClass())) {
            throw new GeneralException(ReservationErrorCode.SEAT_CLASS_MISMATCH);
        }
        if (orderedSeats.stream().anyMatch(seat -> occupiedSeatIds.contains(seat.getId()))) {
            throw new GeneralException(ReservationErrorCode.SEAT_ALREADY_RESERVED);
        }
        return orderedSeats;
    }

    private List<Seat> assignSeats(
            Schedule schedule,
            SeatClass seatClass,
            int passengerCount,
            Set<Long> occupiedSeatIds
    ) {
        List<Seat> availableSeats = seatRepository
                .findSeatsByTrainAndSeatClass(schedule.getTrain(), seatClass)
                .stream()
                .filter(seat -> !occupiedSeatIds.contains(seat.getId()))
                .toList();
        if (availableSeats.size() < passengerCount) {
            throw new GeneralException(ReservationErrorCode.INSUFFICIENT_SEATS);
        }

        Map<Integer, List<Seat>> seatsByCoach = availableSeats.stream()
                .collect(Collectors.groupingBy(
                        seat -> seat.getCoach().getCoachNumber(),
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)
                ));
        return seatsByCoach.values().stream()
                .filter(seats -> seats.size() >= passengerCount)
                .findFirst()
                .map(seats -> seats.subList(0, passengerCount))
                .orElseGet(() -> availableSeats.subList(0, passengerCount));
    }
}
