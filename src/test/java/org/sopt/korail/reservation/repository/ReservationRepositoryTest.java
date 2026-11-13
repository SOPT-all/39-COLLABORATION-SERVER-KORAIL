package org.sopt.korail.reservation.repository;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.config.JpaConfig;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.member.domain.Member;
import org.sopt.korail.reservation.domain.Reservation;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.domain.Station;
import org.sopt.korail.schedule.domain.Train;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@DisplayName("ReservationRepository 테스트")
class ReservationRepositoryTest {

    private static final Long NOT_EXIST_ID = 999L;
    private static final LocalDateTime DEPARTURE_AT = LocalDateTime.of(2026, 12, 1, 9, 0);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReservationRepository reservationRepository;

    @Nested
    @DisplayName("예약 상세 조회(findDetailById)")
    class FindDetailById {

        @Test
        @DisplayName("예약이 있으면 스케줄, 열차, 출발역, 도착역을 함께 조회한다")
        void 예약있음_연관엔티티함께조회() {
            Reservation saved = persistReservation();
            em.flush();
            em.clear();

            Optional<Reservation> result = reservationRepository.findDetailById(saved.getId());

            assertThat(result).hasValueSatisfying(reservation -> {
                Schedule schedule = reservation.getSchedule();
                assertThat(Hibernate.isInitialized(schedule)).isTrue();
                assertThat(Hibernate.isInitialized(schedule.getTrain())).isTrue();
                assertThat(Hibernate.isInitialized(schedule.getDepartureStation())).isTrue();
                assertThat(Hibernate.isInitialized(schedule.getArrivalStation())).isTrue();
            });
        }

        @Test
        @DisplayName("예약이 없으면 빈 Optional을 반환한다")
        void 예약없음_빈결과() {
            assertThat(reservationRepository.findDetailById(NOT_EXIST_ID)).isEmpty();
        }
    }

    private Reservation persistReservation() {
        Station seoul = em.persist(Station.builder().name("서울").major(true).build());
        Station busan = em.persist(Station.builder().name("부산").major(true).build());
        Train train = em.persist(Train.builder().trainNumber("KTX001").trainType(TrainType.KTX).build());
        Schedule schedule = em.persist(Schedule.builder()
                .departureStation(seoul)
                .arrivalStation(busan)
                .train(train)
                .departureAt(DEPARTURE_AT)
                .arrivalAt(DEPARTURE_AT.plusHours(3))
                .build());
        Member member = em.persist(BeanUtils.instantiateClass(Member.class));

        return em.persist(Reservation.builder()
                .schedule(schedule)
                .member(member)
                .status(ReservationStatus.PENDING)
                .totalPrice(59_800)
                .expiresAt(DEPARTURE_AT.minusDays(1))
                .build());
    }
}
