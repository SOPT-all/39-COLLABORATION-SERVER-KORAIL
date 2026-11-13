package org.sopt.korail.reservation.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.config.JpaConfig;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.member.domain.Member;
import org.sopt.korail.reservation.domain.Reservation;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.reservation.domain.Ticket;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.domain.Station;
import org.sopt.korail.schedule.domain.Train;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Direction;
import org.sopt.korail.seat.domain.Seat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@DisplayName("TicketRepository 테스트")
class TicketRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final LocalDateTime BEFORE_NOW = NOW.minusMinutes(1);
    private static final LocalDateTime AFTER_NOW = NOW.plusMinutes(10);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TicketRepository ticketRepository;

    private Train train;
    private Station seoul;
    private Station busan;
    private Schedule schedule;
    private Coach coach;
    private Seat seat;
    private Member member;

    @BeforeEach
    void setUp() {
        seoul = em.persist(Station.builder().name("서울").major(true).build());
        busan = em.persist(Station.builder().name("부산").major(true).build());
        train = em.persist(Train.builder().trainNumber("KTX001").trainType(TrainType.KTX).build());
        schedule = persistSchedule();
        coach = persistCoach(1);
        seat = persistSeat(coach, "A");
        member = em.persist(BeanUtils.instantiateClass(Member.class));
    }

    @Nested
    @DisplayName("점유 좌석 ID 조회(findOccupiedSeatIds)")
    class FindOccupiedSeatIds {

        @Test
        @DisplayName("결제 완료 예약의 좌석은 결제 기한과 상관없이 반환한다")
        void 결제완료예약_좌석반환() {
            ticket(reservation(schedule, ReservationStatus.PAID, BEFORE_NOW), seat, null);

            assertThat(find()).containsExactly(seat.getId());
        }

        @Test
        @DisplayName("결제 기한이 남은 대기 예약의 좌석을 반환한다")
        void 유효한대기예약_좌석반환() {
            ticket(reservation(schedule, ReservationStatus.PENDING, AFTER_NOW), seat, null);

            assertThat(find()).containsExactly(seat.getId());
        }

        @Test
        @DisplayName("결제 기한이 지난 대기 예약의 좌석은 반환하지 않는다")
        void 만료된대기예약_제외() {
            ticket(reservation(schedule, ReservationStatus.PENDING, BEFORE_NOW), seat, null);

            assertThat(find()).isEmpty();
        }

        @Test
        @DisplayName("결제 기한 시각과 현재 시각이 같으면 만료로 보고 반환하지 않는다")
        void 결제기한경계_제외() {
            ticket(reservation(schedule, ReservationStatus.PENDING, NOW), seat, null);

            assertThat(find()).isEmpty();
        }

        @Test
        @DisplayName("취소된 예약의 좌석은 결제 기한이 남아도 반환하지 않는다")
        void 취소된예약_제외() {
            ticket(reservation(schedule, ReservationStatus.CANCELED, AFTER_NOW), seat, null);

            assertThat(find()).isEmpty();
        }

        @Test
        @DisplayName("만료 상태 예약의 좌석은 결제 기한이 남아도 반환하지 않는다")
        void 만료상태예약_제외() {
            ticket(reservation(schedule, ReservationStatus.EXPIRED, AFTER_NOW), seat, null);

            assertThat(find()).isEmpty();
        }

        @Test
        @DisplayName("반환된 티켓의 좌석은 반환하지 않는다")
        void 반환된티켓_제외() {
            ticket(reservation(schedule, ReservationStatus.PAID, AFTER_NOW), seat, NOW);

            assertThat(find()).isEmpty();
        }

        @Test
        @DisplayName("다른 스케줄의 예약 좌석은 반환하지 않는다")
        void 다른스케줄예약_제외() {
            ticket(reservation(persistSchedule(), ReservationStatus.PAID, AFTER_NOW), seat, null);

            assertThat(find()).isEmpty();
        }
    }

    @Nested
    @DisplayName("예약의 유효 티켓 조회(findActiveTicketsByReservationId)")
    class FindActiveTicketsByReservationId {

        @Test
        @DisplayName("반환되지 않은 티켓만 호차, 행, 열 순으로 반환한다")
        void 유효티켓_호차행열순정렬() {
            Reservation reservation = reservation(schedule, ReservationStatus.PAID, AFTER_NOW);
            Seat secondCoachSeat = persistSeat(persistCoach(2), "A");
            Seat firstCoachSeatB = persistSeat(coach, "B");
            Seat returnedSeat = persistSeat(coach, "C");
            ticket(reservation, secondCoachSeat, null);
            ticket(reservation, firstCoachSeatB, null);
            ticket(reservation, seat, null);
            ticket(reservation, returnedSeat, NOW);
            ticket(reservation(schedule, ReservationStatus.PAID, AFTER_NOW), persistSeat(coach, "D"), null);
            em.flush();
            em.clear();

            List<Ticket> result = ticketRepository.findActiveTicketsByReservationId(reservation.getId());

            assertThat(result)
                    .extracting(ticket -> ticket.getSeat().getId())
                    .containsExactly(seat.getId(), firstCoachSeatB.getId(), secondCoachSeat.getId());
        }
    }

    private List<Long> find() {
        em.flush();
        em.clear();
        return ticketRepository.findOccupiedSeatIds(schedule.getId(), NOW);
    }

    private Schedule persistSchedule() {
        return em.persist(Schedule.builder()
                .departureStation(seoul)
                .arrivalStation(busan)
                .train(train)
                .departureAt(NOW.plusDays(1))
                .arrivalAt(NOW.plusDays(1).plusHours(3))
                .build());
    }

    private Coach persistCoach(int coachNumber) {
        return em.persist(Coach.builder()
                .train(train)
                .coachNumber(coachNumber)
                .seatClass(SeatClass.STANDARD)
                .build());
    }

    private Seat persistSeat(Coach coach, String col) {
        return em.persist(Seat.builder()
                .coach(coach)
                .direction(Direction.FORWARD)
                .seatRow(1)
                .seatCol(col)
                .build());
    }

    private Reservation reservation(Schedule schedule, ReservationStatus status, LocalDateTime expiresAt) {
        return em.persist(Reservation.builder()
                .schedule(schedule)
                .member(member)
                .status(status)
                .totalPrice(59800)
                .expiresAt(expiresAt)
                .build());
    }

    private void ticket(Reservation reservation, Seat seat, LocalDateTime returnedAt) {
        Ticket ticket = Ticket.builder()
                .reservation(reservation)
                .seat(seat)
                .passengerType(PassengerType.ADULT)
                .price(59800)
                .build();
        ReflectionTestUtils.setField(ticket, "returnedAt", returnedAt);
        em.persist(ticket);
    }
}
