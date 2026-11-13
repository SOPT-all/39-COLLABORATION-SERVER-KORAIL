package org.sopt.korail.seat.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.config.JpaConfig;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Train;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Direction;
import org.sopt.korail.seat.domain.Seat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
@DisplayName("SeatRepository 테스트")
class SeatRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SeatRepository seatRepository;

    @Nested
    @DisplayName("호차 좌석 조회(findSeatsByCoach)")
    class FindSeatsByCoach {

        @Test
        @DisplayName("해당 호차의 좌석만 행 오름차순, 열 오름차순으로 반환한다")
        void 호차좌석_행열순정렬() {
            Train train = em.persist(Train.builder().trainNumber("KTX001").trainType(TrainType.KTX).build());
            Coach coach = persistCoach(train, 1);
            Coach otherCoach = persistCoach(train, 2);
            persistSeat(coach, 2, "A");
            persistSeat(coach, 1, "B");
            persistSeat(coach, 1, "A");
            persistSeat(otherCoach, 1, "A");
            em.flush();
            em.clear();

            List<Seat> result = seatRepository.findSeatsByCoach(coach);

            assertThat(result)
                    .extracting(seat -> seat.getSeatRow() + seat.getSeatCol())
                    .containsExactly("1A", "1B", "2A");
        }
    }

    @Nested
    @DisplayName("열차의 좌석 등급별 좌석 조회(findSeatsByTrainAndSeatClass)")
    class FindSeatsByTrainAndSeatClass {

        @Test
        @DisplayName("해당 열차, 해당 등급의 좌석만 호차, 행, 열 순으로 반환한다")
        void 열차등급좌석_호차행열순정렬() {
            Train train = persistTrain("KTX001");
            Train otherTrain = persistTrain("KTX002");
            Coach coach2 = persistCoach(train, 2);
            Coach coach1 = persistCoach(train, 1);
            Coach firstClassCoach = persistCoach(train, 3, SeatClass.FIRST);
            persistSeat(coach2, 1, "A");
            persistSeat(coach1, 2, "A");
            persistSeat(coach1, 1, "B");
            persistSeat(coach1, 1, "A");
            persistSeat(firstClassCoach, 1, "A");
            persistSeat(persistCoach(otherTrain, 1), 1, "A");
            em.flush();
            em.clear();

            List<Seat> result = seatRepository.findSeatsByTrainAndSeatClass(train, SeatClass.STANDARD);

            assertThat(result)
                    .extracting(seat -> seat.getCoach().getCoachNumber() + "-" + seat.getSeatRow() + seat.getSeatCol())
                    .containsExactly("1-1A", "1-1B", "1-2A", "2-1A");
        }
    }

    private Train persistTrain(String trainNumber) {
        return em.persist(Train.builder().trainNumber(trainNumber).trainType(TrainType.KTX).build());
    }

    private Coach persistCoach(Train train, int coachNumber) {
        return persistCoach(train, coachNumber, SeatClass.STANDARD);
    }

    private Coach persistCoach(Train train, int coachNumber, SeatClass seatClass) {
        return em.persist(Coach.builder()
                .train(train)
                .coachNumber(coachNumber)
                .seatClass(seatClass)
                .build());
    }

    private void persistSeat(Coach coach, int row, String col) {
        em.persist(Seat.builder()
                .coach(coach)
                .direction(Direction.FORWARD)
                .seatRow(row)
                .seatCol(col)
                .build());
    }
}
