package org.sopt.korail.reservation.service;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.CommonErrorCode;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.member.domain.Member;
import org.sopt.korail.member.repository.MemberRepository;
import org.sopt.korail.reservation.code.ReservationErrorCode;
import org.sopt.korail.reservation.domain.Reservation;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.reservation.domain.Ticket;
import org.sopt.korail.reservation.dto.CreateReservationRequest;
import org.sopt.korail.reservation.dto.CreateReservationResponse;
import org.sopt.korail.reservation.dto.ReservationDetailResponse;
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

import java.time.Duration;
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
public class ReservationService {

    private static final long FIXED_MEMBER_ID = 1L;
    private static final int PAYMENT_TIMEOUT_MINUTES = 10;

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final ScheduleRepository scheduleRepository;
    private final FareRepository fareRepository;
    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CreateReservationResponse createReservation(CreateReservationRequest request) {
        validateSupportedSeatClass(request.seatClass());

        // 같은 일정의 좌석 점유 확인과 티켓 생성을 직렬화해 동시 중복 예약을 막는다.
        Schedule schedule = scheduleRepository.findByIdForUpdate(request.scheduleId())
                .orElseThrow(() -> new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        Set<Long> occupiedSeatIds = new HashSet<>(
                ticketRepository.findOccupiedSeatIds(schedule.getId(), now)
        );

        List<Seat> seats = request.seatIds() == null
                ? assignSeats(schedule, request.seatClass(), request.passengerCount(), occupiedSeatIds)
                : validateAndGetSelectedSeats(schedule, request, occupiedSeatIds);

        Fare fare = fareRepository
                .findByDepartureStationIdAndArrivalStationIdAndTrainTypeAndSeatClassAndPassengerType(
                        schedule.getDepartureStation().getId(),
                        schedule.getArrivalStation().getId(),
                        schedule.getTrain().getTrainType(),
                        request.seatClass(),
                        PassengerType.ADULT
                )
                .orElseThrow(() -> new GeneralException(CommonErrorCode.INTERNAL_SERVER_ERROR));

        int totalPrice;
        try {
            totalPrice = Math.multiplyExact(fare.getPrice(), request.passengerCount());
        } catch (ArithmeticException e) {
            throw new GeneralException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }

        Member member = memberRepository.getReferenceById(FIXED_MEMBER_ID);
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

        return new CreateReservationResponse(reservation.getId());
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findDetailById(reservationId)
                .orElseThrow(() -> new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        List<Ticket> tickets = ticketRepository.findActiveTicketsByReservationId(reservationId);

        Schedule schedule = reservation.getSchedule();
        ReservationStatus responseStatus = reservation.getStatus() == ReservationStatus.PENDING
                && !reservation.getExpiresAt().isAfter(LocalDateTime.now())
                ? ReservationStatus.EXPIRED
                : reservation.getStatus();

        ReservationDetailResponse.ScheduleResponse scheduleResponse =
                new ReservationDetailResponse.ScheduleResponse(
                        schedule.getId(),
                        schedule.getTrain().getTrainNumber(),
                        schedule.getTrain().getTrainType(),
                        trainTypeName(schedule.getTrain().getTrainType()),
                        schedule.getDepartureStation().getName(),
                        schedule.getArrivalStation().getName(),
                        schedule.getDepartureAt(),
                        schedule.getArrivalAt(),
                        Duration.between(schedule.getDepartureAt(), schedule.getArrivalAt()).toMinutes(),
                        schedule.getRemark()
                );

        List<ReservationDetailResponse.TicketResponse> ticketResponses = tickets.stream()
                .map(ticket -> {
                    Seat seat = ticket.getSeat();
                    return new ReservationDetailResponse.TicketResponse(
                            ticket.getId(),
                            seat.getCoach().getCoachNumber(),
                            seat.getCoach().getSeatClass(),
                            seat.getId(),
                            seat.getSeatRow() + seat.getSeatCol(),
                            seat.getDirection(),
                            ticket.getPassengerType(),
                            ticket.getPrice()
                    );
                })
                .toList();

        return new ReservationDetailResponse(
                reservation.getId(),
                responseStatus,
                reservation.getTotalPrice(),
                reservation.getExpiresAt(),
                reservation.getCreatedAt(),
                scheduleResponse,
                ticketResponses
        );
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
                .findAllByTrainIdAndSeatClass(schedule.getTrain().getId(), seatClass)
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

    private void validateSupportedSeatClass(SeatClass seatClass) {
        if (seatClass != SeatClass.STANDARD) {
            throw new GeneralException(ReservationErrorCode.UNSUPPORTED_SEAT_CLASS);
        }
    }

    private String trainTypeName(TrainType trainType) {
        return switch (trainType) {
            case KTX -> "KTX";
            case ITX -> "ITX-새마을";
            case MUGUNGHWA -> "무궁화";
        };
    }
}
