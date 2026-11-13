package org.sopt.korail.schedule.repository;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.config.JpaConfig;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.domain.Station;
import org.sopt.korail.schedule.domain.Train;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaConfig.class)
@DisplayName("ScheduleRepository 테스트")
class ScheduleRepositoryTest {

    private static final Long NOT_EXIST_ID = 999L;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Nested
    @DisplayName("스케줄 조회(getByIdOrThrow)")
    class GetByIdOrThrow {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("스케줄이 있으면 반환한다")
            void 스케줄있음_반환() {
                Schedule saved = persistSchedule();

                Schedule result = scheduleRepository.getByIdOrThrow(saved.getId());

                assertThat(result.getId()).isEqualTo(saved.getId());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("스케줄이 없으면 SCHEDULE_NOT_FOUND를 던진다")
            void 스케줄없음_예외() {
                assertThatThrownBy(() -> scheduleRepository.getByIdOrThrow(NOT_EXIST_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("비관적 락 스케줄 조회(findByIdForUpdate)")
    class FindByIdForUpdate {

        @Test
        @DisplayName("스케줄이 있으면 열차와 출발역, 도착역을 함께 조회한다")
        void 스케줄있음_연관엔티티함께조회() {
            Schedule saved = persistSchedule();
            em.flush();
            em.clear();

            Optional<Schedule> result = scheduleRepository.findByIdForUpdate(saved.getId());

            assertThat(result).hasValueSatisfying(schedule -> {
                assertThat(Hibernate.isInitialized(schedule.getTrain())).isTrue();
                assertThat(Hibernate.isInitialized(schedule.getDepartureStation())).isTrue();
                assertThat(Hibernate.isInitialized(schedule.getArrivalStation())).isTrue();
            });
        }

        @Test
        @DisplayName("스케줄이 없으면 빈 Optional을 반환한다")
        void 스케줄없음_빈결과() {
            assertThat(scheduleRepository.findByIdForUpdate(NOT_EXIST_ID)).isEmpty();
        }
    }

    private Schedule persistSchedule() {
        Station seoul = em.persist(Station.builder().name("서울").major(true).build());
        Station busan = em.persist(Station.builder().name("부산").major(true).build());
        Train train = em.persist(Train.builder().trainNumber("KTX001").trainType(TrainType.KTX).build());
        LocalDateTime departureAt = LocalDateTime.of(2026, 12, 1, 9, 0);

        return em.persist(Schedule.builder()
                .departureStation(seoul)
                .arrivalStation(busan)
                .train(train)
                .departureAt(departureAt)
                .arrivalAt(departureAt.plusHours(3))
                .build());
    }
}
