package org.sopt.korail.support.fixture;

import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Fare;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.domain.Station;
import org.sopt.korail.schedule.domain.Train;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public final class ScheduleFixture {
    public static final Long SCHEDULE_ID = 1L;
    public static final Long TRAIN_ID = 1L;
    public static final LocalDateTime DEPARTURE_AT = LocalDateTime.of(2026, 12, 1, 9, 0);
    public static final LocalDateTime ARRIVAL_AT = DEPARTURE_AT.plusHours(3);

    private ScheduleFixture() {
    }

    public static Schedule schedule() {
        return schedule(SCHEDULE_ID);
    }

    public static Schedule schedule(Long id) {
        Schedule schedule = Schedule.builder()
                .departureStation(station("서울"))
                .arrivalStation(station("부산"))
                .train(train())
                .departureAt(DEPARTURE_AT)
                .arrivalAt(ARRIVAL_AT)
                .build();
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private static Station station(String name) {
        return Station.builder()
                .name(name)
                .major(true)
                .build();
    }

    public static Train train() {
        return train(TRAIN_ID);
    }

    public static Train train(Long id) {
        Train train = Train.builder()
                .trainNumber("KTX" + id)
                .trainType(TrainType.KTX)
                .build();
        ReflectionTestUtils.setField(train, "id", id);
        return train;
    }

    public static Fare fare(int price) {
        return Fare.builder()
                .departureStation(station("서울"))
                .arrivalStation(station("부산"))
                .trainType(TrainType.KTX)
                .seatClass(SeatClass.STANDARD)
                .passengerType(PassengerType.ADULT)
                .price(price)
                .build();
    }
}
