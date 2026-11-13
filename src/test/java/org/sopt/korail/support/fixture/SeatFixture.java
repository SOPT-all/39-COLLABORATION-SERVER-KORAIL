package org.sopt.korail.support.fixture;

import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.schedule.domain.Train;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Direction;
import org.sopt.korail.seat.domain.Seat;
import org.springframework.test.util.ReflectionTestUtils;

public final class SeatFixture {
    public static final Long COACH_ID = 10L;

    private SeatFixture() {
    }

    public static Coach coach(Train train) {
        return coach(COACH_ID, train, 1, SeatClass.STANDARD);
    }

    public static Coach coach(Long id, Train train, int coachNumber, SeatClass seatClass) {
        Coach coach = Coach.builder()
                .train(train)
                .coachNumber(coachNumber)
                .seatClass(seatClass)
                .build();
        ReflectionTestUtils.setField(coach, "id", id);
        return coach;
    }

    public static Seat seat(Coach coach, Long id, int row, String col) {
        Seat seat = Seat.builder()
                .coach(coach)
                .direction(Direction.FORWARD)
                .seatRow(row)
                .seatCol(col)
                .build();
        ReflectionTestUtils.setField(seat, "id", id);
        return seat;
    }
}
