package org.sopt.korail.schedule.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.config.JpaConfig;
import org.sopt.korail.global.config.QuerydslConfig;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.domain.Station;
import org.sopt.korail.schedule.domain.Train;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class, QuerydslConfig.class, ScheduleQueryRepository.class})
@DisplayName("ScheduleQueryRepository 테스트")
class ScheduleQueryRepositoryTest {

    private static final String DEPARTURE = "서울";
    private static final String ARRIVAL = "부산";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 12, 0);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ScheduleQueryRepository scheduleQueryRepository;

    private Station seoul;
    private Station busan;

    @BeforeEach
    void setUp() {
        seoul = em.persist(Station.builder().name(DEPARTURE).major(true).build());
        busan = em.persist(Station.builder().name(ARRIVAL).major(true).build());
    }

    @Nested
    @DisplayName("열차 목록 조회(findSchedules)")
    class FindSchedules {

        @Test
        @DisplayName("이미 출발한 열차는 조회되지 않는다")
        void 과거열차_제외() {
            persistSchedule(NOW.minusHours(1), TrainType.KTX);
            Schedule upcoming = persistSchedule(NOW.plusHours(1), TrainType.KTX);

            List<Schedule> result = scheduleQueryRepository
                    .findSchedules(DEPARTURE, ARRIVAL, null, null, NOW);

            assertThat(result).extracting(Schedule::getId).containsExactly(upcoming.getId());
        }

        @Test
        @DisplayName("출발일을 오늘로 지정해도 이미 출발한 열차는 조회되지 않는다")
        void 오늘지정_지난시각제외() {
            persistSchedule(NOW.minusHours(3), TrainType.KTX);
            Schedule upcoming = persistSchedule(NOW.plusHours(3), TrainType.KTX);

            List<Schedule> result = scheduleQueryRepository
                    .findSchedules(DEPARTURE, ARRIVAL, NOW.toLocalDate(), null, NOW);

            assertThat(result).extracting(Schedule::getId).containsExactly(upcoming.getId());
        }

        @Test
        @DisplayName("출발일을 지정하면 해당 날짜의 열차만 조회된다")
        void 출발일필터() {
            Schedule today = persistSchedule(NOW.plusHours(1), TrainType.KTX);
            persistSchedule(NOW.plusDays(1), TrainType.KTX);

            List<Schedule> result = scheduleQueryRepository
                    .findSchedules(DEPARTURE, ARRIVAL, NOW.toLocalDate(), null, NOW);

            assertThat(result).extracting(Schedule::getId).containsExactly(today.getId());
        }

        @Test
        @DisplayName("열차 종류를 지정하면 해당 종류만 조회된다")
        void 열차종류필터() {
            persistSchedule(NOW.plusHours(1), TrainType.KTX);
            Schedule itx = persistSchedule(NOW.plusHours(2), TrainType.ITX);

            List<Schedule> result = scheduleQueryRepository
                    .findSchedules(DEPARTURE, ARRIVAL, null, List.of(TrainType.ITX), NOW);

            assertThat(result).extracting(Schedule::getId).containsExactly(itx.getId());
        }

        @Test
        @DisplayName("출발 시각 오름차순으로 정렬된다")
        void 출발시각정렬() {
            Schedule late = persistSchedule(NOW.plusHours(5), TrainType.KTX);
            Schedule early = persistSchedule(NOW.plusHours(1), TrainType.KTX);

            List<Schedule> result = scheduleQueryRepository
                    .findSchedules(DEPARTURE, ARRIVAL, null, null, NOW);

            assertThat(result).extracting(Schedule::getId)
                    .containsExactly(early.getId(), late.getId());
        }
    }

    private Schedule persistSchedule(LocalDateTime departureAt, TrainType trainType) {
        Train train = em.persist(Train.builder()
                .trainNumber(trainType.name() + departureAt.getHour())
                .trainType(trainType)
                .build());

        return em.persist(Schedule.builder()
                .departureStation(seoul)
                .arrivalStation(busan)
                .train(train)
                .departureAt(departureAt)
                .arrivalAt(departureAt.plusHours(3))
                .build());
    }
}
