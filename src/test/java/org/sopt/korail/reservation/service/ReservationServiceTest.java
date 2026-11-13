package org.sopt.korail.reservation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.member.code.MemberErrorCode;
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
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.repository.FareRepository;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.sopt.korail.seat.code.SeatErrorCode;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Seat;
import org.sopt.korail.seat.repository.SeatRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sopt.korail.global.config.TimeConfig.KST;
import static org.sopt.korail.support.fixture.MemberFixture.MEMBER_ID;
import static org.sopt.korail.support.fixture.MemberFixture.OTHER_MEMBER_ID;
import static org.sopt.korail.support.fixture.MemberFixture.member;
import static org.sopt.korail.support.fixture.ReservationFixture.RESERVATION_ID;
import static org.sopt.korail.support.fixture.ReservationFixture.TICKET_ID;
import static org.sopt.korail.support.fixture.ReservationFixture.reservation;
import static org.sopt.korail.support.fixture.ReservationFixture.ticket;
import static org.sopt.korail.support.fixture.ScheduleFixture.SCHEDULE_ID;
import static org.sopt.korail.support.fixture.ScheduleFixture.fare;
import static org.sopt.korail.support.fixture.ScheduleFixture.schedule;
import static org.sopt.korail.support.fixture.ScheduleFixture.train;
import static org.sopt.korail.support.fixture.SeatFixture.coach;
import static org.sopt.korail.support.fixture.SeatFixture.seat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService 단위 테스트")
class ReservationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW.atZone(KST).toInstant(), KST);

    private ReservationService reservationService;

    @Mock private ReservationRepository reservationRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private FareRepository fareRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, ticketRepository, scheduleRepository,
                fareRepository, seatRepository, memberRepository, FIXED_CLOCK);
    }

    @Nested
    @DisplayName("예약 생성(createReservation)")
    class CreateReservation {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("선택한 빈 좌석이면 PENDING 예약과 인원 수만큼 티켓을 생성한다")
            void 선택좌석_예약생성성공() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                Schedule schedule = schedule();
                Coach coach = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                List<Seat> seats = List.of(seat(coach, 101L, 1, "A"), seat(coach, 102L, 1, "B"));
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, List.of(101L, 102L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findAllByIdIn(request.seatIds())).willReturn(seats);
                given(fareRepository.findFare(any(), any(), any(), any(), any())).willReturn(Optional.of(fare(59_800)));
                                given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> {
                    Reservation saved = inv.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", RESERVATION_ID);
                    return saved;
                });

                CreateReservationResponse result = reservationService.createReservation(MEMBER_ID, request);

                assertThat(result.reservationId()).isEqualTo(RESERVATION_ID);
                ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
                verify(reservationRepository).save(reservationCaptor.capture());
                assertThat(reservationCaptor.getValue().getStatus()).isEqualTo(ReservationStatus.PENDING);
                assertThat(reservationCaptor.getValue().getTotalPrice()).isEqualTo(119_600);
                assertThat(reservationCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plusMinutes(10));
                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<Ticket>> ticketCaptor = ArgumentCaptor.forClass(List.class);
                verify(ticketRepository).saveAll(ticketCaptor.capture());
                assertThat(ticketCaptor.getValue()).hasSize(2)
                        .allSatisfy(ticket -> assertThat(ticket.getPrice()).isEqualTo(59_800));
            }

            @Test
            @DisplayName("자동 배정하면 전원을 수용할 수 있는 가장 앞 호차에 함께 배정한다")
            void 자동배정_같은호차우선() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                Schedule schedule = schedule();
                Coach coach1 = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                Coach coach2 = coach(2L, schedule.getTrain(), 2, SeatClass.STANDARD);
                List<Seat> seats = List.of(
                        seat(coach1, 101L, 1, "A"),
                        seat(coach2, 201L, 1, "A"),
                        seat(coach2, 202L, 1, "B")
                );
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, null);
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findSeatsByTrainAndSeatClass(schedule.getTrain(), SeatClass.STANDARD))
                        .willReturn(seats);
                given(fareRepository.findFare(any(), any(), any(), any(), any())).willReturn(Optional.of(fare(59_800)));
                                given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> inv.getArgument(0));

                reservationService.createReservation(MEMBER_ID, request);

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<Ticket>> ticketCaptor = ArgumentCaptor.forClass(List.class);
                verify(ticketRepository).saveAll(ticketCaptor.capture());
                assertThat(ticketCaptor.getValue())
                        .extracting(ticket -> ticket.getSeat().getCoach().getCoachNumber())
                        .containsExactly(2, 2);
            }

            @Test
            @DisplayName("한 호차에 전원을 배정할 수 없으면 호차 순서대로 분산 배정한다")
            void 자동배정_여러호차분산() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                Schedule schedule = schedule();
                Coach coach1 = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                Coach coach2 = coach(2L, schedule.getTrain(), 2, SeatClass.STANDARD);
                List<Seat> seats = List.of(
                        seat(coach1, 101L, 1, "A"),
                        seat(coach2, 201L, 1, "A")
                );
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, null);
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findSeatsByTrainAndSeatClass(schedule.getTrain(), SeatClass.STANDARD))
                        .willReturn(seats);
                given(fareRepository.findFare(any(), any(), any(), any(), any())).willReturn(Optional.of(fare(59_800)));
                                given(reservationRepository.save(any(Reservation.class))).willAnswer(inv -> inv.getArgument(0));

                reservationService.createReservation(MEMBER_ID, request);

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<Ticket>> ticketCaptor = ArgumentCaptor.forClass(List.class);
                verify(ticketRepository).saveAll(ticketCaptor.capture());
                assertThat(ticketCaptor.getValue())
                        .extracting(ticket -> ticket.getSeat().getCoach().getCoachNumber())
                        .containsExactly(1, 2);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND를 던진다")
            void 생성시_회원없음_예외() {
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 1, null);
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
                verify(scheduleRepository, never()).findByIdForUpdate(any());
            }

            @Test
            @DisplayName("존재하지 않는 일정이면 SCHEDULE_NOT_FOUND를 던진다")
            void 없는일정_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 1, null);
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
            }

            @Test
            @DisplayName("선택한 좌석 수가 인원 수와 다르면 SEAT_COUNT_MISMATCH를 던진다")
            void 좌석수불일치_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, List.of(101L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule()));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.SEAT_COUNT_MISMATCH));
            }

            @Test
            @DisplayName("중복된 좌석 ID가 있으면 SEAT_COUNT_MISMATCH를 던진다")
            void 중복좌석_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, List.of(101L, 101L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule()));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.SEAT_COUNT_MISMATCH));
            }

            @Test
            @DisplayName("존재하지 않는 좌석이면 SEAT_NOT_FOUND를 던진다")
            void 없는좌석_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 1, List.of(999L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule()));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findAllByIdIn(request.seatIds())).willReturn(List.of());

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(SeatErrorCode.SEAT_NOT_FOUND));
            }

            @Test
            @DisplayName("다른 열차의 좌석이면 SEAT_NOT_IN_SCHEDULE을 던진다")
            void 다른열차좌석_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                Coach otherTrainCoach = coach(1L, train(999L), 1, SeatClass.STANDARD);
                Seat seat = seat(otherTrainCoach, 101L, 1, "A");
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 1, List.of(101L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule()));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findAllByIdIn(request.seatIds())).willReturn(List.of(seat));

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.SEAT_NOT_IN_SCHEDULE));
            }

            @Test
            @DisplayName("요청한 좌석 등급과 다른 좌석이면 SEAT_CLASS_MISMATCH를 던진다")
            void 다른좌석등급_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                Schedule schedule = schedule();
                Coach firstClassCoach = coach(1L, schedule.getTrain(), 1, SeatClass.FIRST);
                Seat seat = seat(firstClassCoach, 101L, 1, "A");
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 1, List.of(101L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findAllByIdIn(request.seatIds())).willReturn(List.of(seat));

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.SEAT_CLASS_MISMATCH));
            }

            @Test
            @DisplayName("선택한 좌석이 점유 중이면 SEAT_ALREADY_RESERVED를 던진다")
            void 점유좌석_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                Schedule schedule = schedule();
                Coach coach = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                Seat seat = seat(coach, 101L, 1, "A");
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 1, List.of(101L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of(101L));
                given(seatRepository.findAllByIdIn(request.seatIds())).willReturn(List.of(seat));

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.SEAT_ALREADY_RESERVED));
            }

            @Test
            @DisplayName("자동 배정할 빈 좌석이 부족하면 INSUFFICIENT_SEATS를 던진다")
            void 잔여좌석부족_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                Schedule schedule = schedule();
                Coach coach = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, null);
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findSeatsByTrainAndSeatClass(schedule.getTrain(), SeatClass.STANDARD))
                        .willReturn(List.of(seat(coach, 101L, 1, "A")));

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.INSUFFICIENT_SEATS));
            }

            @Test
            @DisplayName("일치하는 운임이 없으면 FARE_NOT_FOUND를 던진다")
            void 운임없음_예외() {
                given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member()));
                Schedule schedule = schedule();
                Coach coach = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                Seat seat = seat(coach, 101L, 1, "A");
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 1, List.of(101L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findAllByIdIn(request.seatIds())).willReturn(List.of(seat));
                given(fareRepository.findFare(any(), any(), any(), any(), any())).willReturn(Optional.empty());

                assertThatThrownBy(() -> reservationService.createReservation(MEMBER_ID, request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ScheduleErrorCode.FARE_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("예약 상세 조회(getReservation)")
    class GetReservation {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("예약의 열차 정보와 승차권 좌석 정보를 응답한다")
            void 예약상세_조회성공() {
                Schedule schedule = schedule();
                Reservation reservation = reservation(schedule, ReservationStatus.PAID, NOW.plusMinutes(10));
                Coach coach = coach(1L, schedule.getTrain(), 2, SeatClass.STANDARD);
                Ticket ticket = ticket(reservation, seat(coach, 102L, 3, "B"), 59_800);
                given(reservationRepository.findDetailById(RESERVATION_ID)).willReturn(Optional.of(reservation));
                given(ticketRepository.findActiveTicketsByReservationId(RESERVATION_ID)).willReturn(List.of(ticket));

                ReservationDetailResponse result = reservationService.getReservation(MEMBER_ID, RESERVATION_ID);

                assertThat(result.status()).isEqualTo(ReservationStatus.PAID);
                assertThat(result.schedule().trainTypeName()).isEqualTo("KTX");
                assertThat(result.schedule().durationMinutes()).isEqualTo(180);
                assertThat(result.tickets()).singleElement().satisfies(response -> {
                    assertThat(response.ticketId()).isEqualTo(TICKET_ID);
                    assertThat(response.coachNumber()).isEqualTo(2);
                    assertThat(response.seatNumber()).isEqualTo("3B");
                    assertThat(response.price()).isEqualTo(59_800);
                });
            }

            @Test
            @DisplayName("결제 기한이 지난 PENDING 예약이면 EXPIRED로 응답한다")
            void 만료된예약_EXPIRED응답() {
                Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW.minusMinutes(1));
                given(reservationRepository.findDetailById(RESERVATION_ID)).willReturn(Optional.of(reservation));
                given(ticketRepository.findActiveTicketsByReservationId(RESERVATION_ID)).willReturn(List.of());

                ReservationDetailResponse result = reservationService.getReservation(MEMBER_ID, RESERVATION_ID);

                assertThat(result.status()).isEqualTo(ReservationStatus.EXPIRED);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 예약이면 RESERVATION_NOT_FOUND를 던진다")
            void 없는예약_예외() {
                given(reservationRepository.findDetailById(RESERVATION_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> reservationService.getReservation(MEMBER_ID, RESERVATION_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND));
            }

            @Test
            @DisplayName("다른 회원의 예약이면 RESERVATION_NOT_FOUND를 던진다")
            void 다른회원예약_예외() {
                Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW.plusMinutes(10));
                given(reservationRepository.findDetailById(RESERVATION_ID)).willReturn(Optional.of(reservation));

                assertThatThrownBy(() -> reservationService.getReservation(OTHER_MEMBER_ID, RESERVATION_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND));
                verify(ticketRepository, never()).findActiveTicketsByReservationId(any());
            }
        }
    }
}
