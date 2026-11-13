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
    @DisplayName("호차 좌석 조회(findSeats)")
    class FindSeats {

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

            List<Seat> result = seatRepository.findSeats(coach);

            assertThat(result)
                    .extracting(seat -> seat.getSeatRow() + seat.getSeatCol())
                    .containsExactly("1A", "1B", "2A");
        }
    }

    private Coach persistCoach(Train train, int coachNumber) {
        return em.persist(Coach.builder()
                .train(train)
                .coachNumber(coachNumber)
                .seatClass(SeatClass.STANDARD)
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
