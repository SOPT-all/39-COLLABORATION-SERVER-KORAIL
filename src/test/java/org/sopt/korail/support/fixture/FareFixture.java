package org.sopt.korail.support.fixture;

import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Fare;
import org.sopt.korail.schedule.domain.Station;

public final class FareFixture {
    private FareFixture() {
    }

    public static Fare fare(TrainType trainType, SeatClass seatClass, int price) {
        return Fare.builder()
                .departureStation(station("서울"))
                .arrivalStation(station("부산"))
                .trainType(trainType)
                .seatClass(seatClass)
                .price(price)
                .passengerType(PassengerType.ADULT)
                .build();
    }

    private static Station station(String name) {
        return Station.builder()
                .name(name)
                .major(true)
                .build();
    }
}
