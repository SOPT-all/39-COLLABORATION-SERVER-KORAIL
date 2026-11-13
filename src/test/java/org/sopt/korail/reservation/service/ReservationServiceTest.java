package org.sopt.korail.reservation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
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
import org.sopt.korail.schedule.repository.FareRepository;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Seat;
import org.sopt.korail.seat.repository.SeatRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.sopt.korail.support.fixture.ReservationFixture.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService 단위 테스트")
class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock private ReservationRepository reservationRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private FareRepository fareRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private MemberRepository memberRepository;

    @Nested
    @DisplayName("예약 생성")
    class CreateReservation {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("선택한 빈 좌석이면 PENDING 예약과 인원 수만큼 티켓을 생성한다")
            void 선택좌석_예약을_생성한다() {
                var schedule = schedule();
                Coach coach = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                List<Seat> seats = List.of(seat(101L, coach, 1, "A"), seat(102L, coach, 1, "B"));
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, List.of(101L, 102L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findAllByIdIn(request.seatIds())).willReturn(seats);
                given(fareRepository.findByDepartureStationIdAndArrivalStationIdAndTrainTypeAndSeatClassAndPassengerType(
                        any(), any(), any(), any(), any())).willReturn(Optional.of(fare(59_800)));
                given(memberRepository.getReferenceById(1L)).willReturn(member());
                given(reservationRepository.save(any())).willAnswer(invocation -> {
                    Reservation saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", RESERVATION_ID);
                    return saved;
                });

                CreateReservationResponse result = reservationService.createReservation(request);

                assertThat(result.reservationId()).isEqualTo(RESERVATION_ID);
                ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
                verify(reservationRepository).save(reservationCaptor.capture());
                assertThat(reservationCaptor.getValue().getStatus()).isEqualTo(ReservationStatus.PENDING);
                assertThat(reservationCaptor.getValue().getTotalPrice()).isEqualTo(119_600);
                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<Ticket>> ticketCaptor = ArgumentCaptor.forClass(List.class);
                verify(ticketRepository).saveAll(ticketCaptor.capture());
                assertThat(ticketCaptor.getValue()).hasSize(2)
                        .allSatisfy(ticket -> assertThat(ticket.getPrice()).isEqualTo(59_800));
            }

            @Test
            @DisplayName("자동 배정하면 전원을 수용할 수 있는 가장 앞 호차에 함께 배정한다")
            void 자동배정_같은호차를_우선한다() {
                var schedule = schedule();
                Coach coach1 = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                Coach coach2 = coach(2L, schedule.getTrain(), 2, SeatClass.STANDARD);
                List<Seat> seats = List.of(
                        seat(101L, coach1, 1, "A"),
                        seat(201L, coach2, 1, "A"),
                        seat(202L, coach2, 1, "B")
                );
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, null);
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findAllByTrainIdAndSeatClass(schedule.getTrain().getId(), SeatClass.STANDARD))
                        .willReturn(seats);
                given(fareRepository.findByDepartureStationIdAndArrivalStationIdAndTrainTypeAndSeatClassAndPassengerType(
                        any(), any(), any(), any(), any())).willReturn(Optional.of(fare(59_800)));
                given(memberRepository.getReferenceById(1L)).willReturn(member());
                given(reservationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

                reservationService.createReservation(request);

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<Ticket>> ticketCaptor = ArgumentCaptor.forClass(List.class);
                verify(ticketRepository).saveAll(ticketCaptor.capture());
                assertThat(ticketCaptor.getValue())
                        .extracting(ticket -> ticket.getSeat().getCoach().getCoachNumber())
                        .containsExactly(2, 2);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("특실을 요청하면 RESERVATION4004를 던진다")
            void 특실요청_RESERVATION4004를_던진다() {
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.PREMIUM, 1, null);

                assertThatThrownBy(() -> reservationService.createReservation(request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.UNSUPPORTED_SEAT_CLASS));
            }

            @Test
            @DisplayName("선택한 좌석 수가 인원과 다르면 RESERVATION4001을 던진다")
            void 좌석수불일치_RESERVATION4001을_던진다() {
                var schedule = schedule();
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, List.of(101L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());

                assertThatThrownBy(() -> reservationService.createReservation(request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.SEAT_COUNT_MISMATCH));
            }

            @Test
            @DisplayName("선택한 좌석이 점유 중이면 RESERVATION4091을 던진다")
            void 점유좌석_RESERVATION4091을_던진다() {
                var schedule = schedule();
                Coach coach = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                Seat seat = seat(101L, coach, 1, "A");
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 1, List.of(101L));
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of(101L));
                given(seatRepository.findAllByIdIn(request.seatIds())).willReturn(List.of(seat));

                assertThatThrownBy(() -> reservationService.createReservation(request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.SEAT_ALREADY_RESERVED));
            }

            @Test
            @DisplayName("자동 배정할 빈 좌석이 부족하면 RESERVATION4092를 던진다")
            void 잔여좌석부족_RESERVATION4092를_던진다() {
                var schedule = schedule();
                Coach coach = coach(1L, schedule.getTrain(), 1, SeatClass.STANDARD);
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, null);
                given(scheduleRepository.findByIdForUpdate(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(ticketRepository.findOccupiedSeatIds(any(), any())).willReturn(List.of());
                given(seatRepository.findAllByTrainIdAndSeatClass(schedule.getTrain().getId(), SeatClass.STANDARD))
                        .willReturn(List.of(seat(101L, coach, 1, "A")));

                assertThatThrownBy(() -> reservationService.createReservation(request))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ReservationErrorCode.INSUFFICIENT_SEATS));
            }
        }
    }

    @Nested
    @DisplayName("예약 상세 조회")
    class GetReservation {

        @Test
        @DisplayName("결제 기한이 지난 PENDING 예약이면 EXPIRED로 응답한다")
        void 만료된예약_EXPIRED로_응답한다() {
            Reservation reservation = reservation(ReservationStatus.PENDING, LocalDateTime.now().minusMinutes(1));
            given(reservationRepository.findDetailById(RESERVATION_ID)).willReturn(Optional.of(reservation));
            given(ticketRepository.findActiveTicketsByReservationId(RESERVATION_ID)).willReturn(List.of());

            ReservationDetailResponse result = reservationService.getReservation(RESERVATION_ID);

            assertThat(result.status()).isEqualTo(ReservationStatus.EXPIRED);
            assertThat(result.schedule().durationMinutes()).isEqualTo(160);
            assertThat(result.schedule().remark()).isEqualTo("서대구 정차");
        }

        @Test
        @DisplayName("존재하지 않는 예약이면 RESERVATION4041을 던진다")
        void 없는예약_RESERVATION4041을_던진다() {
            given(reservationRepository.findDetailById(RESERVATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservation(RESERVATION_ID))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND));
        }
    }
}
