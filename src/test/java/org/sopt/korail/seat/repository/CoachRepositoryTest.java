package org.sopt.korail.seat.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.config.JpaConfig;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Train;
import org.sopt.korail.seat.domain.Coach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@DisplayName("CoachRepository 테스트")
class CoachRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CoachRepository coachRepository;

    @Nested
    @DisplayName("열차의 좌석 등급별 호차 조회(findCoachesByTrainAndSeatClass)")
    class FindCoachesByTrainAndSeatClass {

        @Test
        @DisplayName("해당 열차, 해당 등급의 호차만 호차 번호 오름차순으로 반환한다")
        void 열차등급호차_호차번호순정렬() {
            Train train = persistTrain("KTX001");
            Train otherTrain = persistTrain("KTX002");
            persistCoach(train, 3, SeatClass.STANDARD);
            persistCoach(train, 1, SeatClass.STANDARD);
            persistCoach(train, 2, SeatClass.FIRST);
            persistCoach(otherTrain, 4, SeatClass.STANDARD);
            em.flush();
            em.clear();

            List<Coach> result = coachRepository.findCoachesByTrainAndSeatClass(train, SeatClass.STANDARD);

            assertThat(result)
                    .extracting(Coach::getCoachNumber)
                    .containsExactly(1, 3);
        }

        @Test
        @DisplayName("해당 등급의 호차가 없으면 빈 목록을 반환한다")
        void 등급호차없음_빈목록() {
            Train train = persistTrain("KTX001");
            persistCoach(train, 1, SeatClass.STANDARD);
            em.flush();
            em.clear();

            List<Coach> result = coachRepository.findCoachesByTrainAndSeatClass(train, SeatClass.FIRST);

            assertThat(result).isEmpty();
        }
    }

    private Train persistTrain(String trainNumber) {
        return em.persist(Train.builder().trainNumber(trainNumber).trainType(TrainType.KTX).build());
    }

    private void persistCoach(Train train, int coachNumber, SeatClass seatClass) {
        em.persist(Coach.builder()
                .train(train)
                .coachNumber(coachNumber)
                .seatClass(seatClass)
                .build());
    }
}
