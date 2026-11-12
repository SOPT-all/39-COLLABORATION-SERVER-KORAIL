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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
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
        @DisplayName("결제 기한이 남은 예약의 티켓 좌석을 반환한다")
        void 유효한예약_좌석반환() {
            ticket(reservation(schedule, AFTER_NOW), seat, null);

            assertThat(find()).containsExactly(seat.getId());
        }

        @Test
        @DisplayName("반환된 티켓의 좌석은 반환하지 않는다")
        void 반환된티켓_제외() {
            ticket(reservation(schedule, AFTER_NOW), seat, NOW);

            assertThat(find()).isEmpty();
        }

        @Test
        @DisplayName("결제 기한이 지난 대기 예약의 좌석은 반환하지 않는다")
        void 만료된대기예약_제외() {
            ticket(reservation(schedule, BEFORE_NOW), seat, null);

            assertThat(find()).isEmpty();
        }

        @Test
        @DisplayName("다른 스케줄의 예약 좌석은 반환하지 않는다")
        void 다른스케줄예약_제외() {
            ticket(reservation(persistSchedule(), AFTER_NOW), seat, null);

            assertThat(find()).isEmpty();
        }

        @Test
        @DisplayName("다른 호차의 좌석은 반환하지 않는다")
        void 다른호차좌석_제외() {
            Seat otherCoachSeat = persistSeat(persistCoach(2), "A");
            ticket(reservation(schedule, AFTER_NOW), otherCoachSeat, null);

            assertThat(find()).isEmpty();
        }
    }

    private List<Long> find() {
        em.flush();
        em.clear();
        return ticketRepository.findOccupiedSeatIds(schedule, coach, NOW);
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

    private Reservation reservation(Schedule schedule, LocalDateTime expiresAt) {
        return em.persist(Reservation.builder()
                .schedule(schedule)
                .member(member)
                .status(ReservationStatus.PENDING)
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
