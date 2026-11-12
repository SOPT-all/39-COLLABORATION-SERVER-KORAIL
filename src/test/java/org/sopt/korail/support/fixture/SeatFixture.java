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
        Coach coach = Coach.builder()
                .train(train)
                .coachNumber(1)
                .seatClass(SeatClass.STANDARD)
                .build();
        ReflectionTestUtils.setField(coach, "id", COACH_ID);
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
