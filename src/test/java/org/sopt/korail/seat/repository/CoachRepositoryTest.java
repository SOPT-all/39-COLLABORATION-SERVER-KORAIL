package org.sopt.korail.seat.repository;

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
import org.sopt.korail.seat.dto.CoachSeatSummary;
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
@DisplayName("CoachRepository 테스트")
class CoachRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final LocalDateTime BEFORE_NOW = NOW.minusMinutes(1);
    private static final LocalDateTime AFTER_NOW = NOW.plusMinutes(10);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CoachRepository coachRepository;

    private Train train;
    private Schedule schedule;
    private Coach coach;
    private Member member;

    @BeforeEach
    void setUp() {
        Station seoul = em.persist(Station.builder().name("서울").major(true).build());
        Station busan = em.persist(Station.builder().name("부산").major(true).build());
        train = em.persist(Train.builder().trainNumber("KTX001").trainType(TrainType.KTX).build());
        schedule = persistSchedule(seoul, busan, train);
        coach = persistCoach(train, 1, SeatClass.STANDARD);
        member = em.persist(BeanUtils.instantiateClass(Member.class));
    }

    @Nested
    @DisplayName("호차별 좌석 집계 조회(findCoachSeatSummaries)")
    class FindCoachSeatSummaries {

        @Nested
        @DisplayName("점유 좌석 계산")
        class OccupiedSeats {

            @Test
            @DisplayName("결제 기한이 남은 예약의 티켓은 점유 좌석으로 센다")
            void 유효한예약_점유로셈() {
                Seat seat = persistSeats(coach, 2).get(0);
                ticket(reservation(schedule, AFTER_NOW), seat, null);

                CoachSeatSummary result = findSingle(schedule);

                assertThat(result.totalSeats()).isEqualTo(2);
                assertThat(result.occupiedSeats()).isEqualTo(1);
            }

            @Test
            @DisplayName("반환된 티켓의 좌석은 점유 좌석으로 세지 않는다")
            void 반환된티켓_점유에서제외() {
                Seat seat = persistSeats(coach, 1).get(0);
                ticket(reservation(schedule, AFTER_NOW), seat, NOW);

                CoachSeatSummary result = findSingle(schedule);

                assertThat(result.occupiedSeats()).isZero();
            }

            @Test
            @DisplayName("결제 기한이 지난 대기 예약의 좌석은 점유 좌석으로 세지 않는다")
            void 만료된대기예약_점유에서제외() {
                Seat seat = persistSeats(coach, 1).get(0);
                ticket(reservation(schedule, BEFORE_NOW), seat, null);

                CoachSeatSummary result = findSingle(schedule);

                assertThat(result.occupiedSeats()).isZero();
            }

            @Test
            @DisplayName("다른 스케줄의 예약은 점유 좌석으로 세지 않는다")
            void 다른스케줄예약_점유에서제외() {
                Seat seat = persistSeats(coach, 1).get(0);
                Schedule otherSchedule = persistSchedule(schedule.getDepartureStation(), schedule.getArrivalStation(), train);
                ticket(reservation(otherSchedule, AFTER_NOW), seat, null);

                CoachSeatSummary result = findSingle(schedule);

                assertThat(result.occupiedSeats()).isZero();
            }
        }

        @Nested
        @DisplayName("호차 조회 범위")
        class CoachScope {

            @Test
            @DisplayName("스케줄 열차의 요청한 좌석 등급 호차만 호차 번호 오름차순으로 반환한다")
            void 열차와좌석등급_필터및정렬() {
                persistCoach(train, 3, SeatClass.STANDARD);
                persistCoach(train, 2, SeatClass.STANDARD);
                persistCoach(train, 10, SeatClass.FIRST);
                Train otherTrain = em.persist(Train.builder().trainNumber("KTX002").trainType(TrainType.KTX).build());
                persistCoach(otherTrain, 4, SeatClass.STANDARD);
                em.flush();
                em.clear();

                List<CoachSeatSummary> result = coachRepository.findCoachSeatSummaries(schedule, SeatClass.STANDARD, NOW);

                assertThat(result).extracting(CoachSeatSummary::coachNumber).containsExactly(1, 2, 3);
            }

            @Test
            @DisplayName("좌석이 없는 호차는 전체 좌석 수와 점유 좌석 수가 0이다")
            void 좌석없는호차_0반환() {
                CoachSeatSummary result = findSingle(schedule);

                assertThat(result.totalSeats()).isZero();
                assertThat(result.occupiedSeats()).isZero();
            }
        }
    }

    private CoachSeatSummary findSingle(Schedule target) {
        em.flush();
        em.clear();
        List<CoachSeatSummary> result = coachRepository.findCoachSeatSummaries(target, SeatClass.STANDARD, NOW);
        assertThat(result).hasSize(1);
        return result.get(0);
    }

    private Schedule persistSchedule(Station departure, Station arrival, Train train) {
        return em.persist(Schedule.builder()
                .departureStation(departure)
                .arrivalStation(arrival)
                .train(train)
                .departureAt(NOW.plusDays(1))
                .arrivalAt(NOW.plusDays(1).plusHours(3))
                .build());
    }

    private Coach persistCoach(Train train, int coachNumber, SeatClass seatClass) {
        return em.persist(Coach.builder()
                .train(train)
                .coachNumber(coachNumber)
                .seatClass(seatClass)
                .build());
    }

    private List<Seat> persistSeats(Coach coach, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> em.persist(Seat.builder()
                        .coach(coach)
                        .direction(Direction.FORWARD)
                        .seatRow(1)
                        .seatCol(String.valueOf((char) ('A' + i)))
                        .build()))
                .toList();
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
