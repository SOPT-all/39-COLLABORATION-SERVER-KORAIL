package org.sopt.korail.support.fixture;

import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.member.domain.Member;
import org.sopt.korail.reservation.domain.Reservation;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.schedule.domain.Fare;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.domain.Station;
import org.sopt.korail.schedule.domain.Train;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Direction;
import org.sopt.korail.seat.domain.Seat;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public final class ReservationFixture {
    public static final long SCHEDULE_ID = 1L;
    public static final long RESERVATION_ID = 10L;
    public static final long TRAIN_ID = 20L;
    public static final LocalDateTime DEPARTURE_AT = LocalDateTime.of(2026, 11, 20, 6, 0);
    public static final LocalDateTime ARRIVAL_AT = LocalDateTime.of(2026, 11, 20, 8, 40);

    private ReservationFixture() {
    }

    public static Station station(long id, String name) {
        Station station = Station.builder().name(name).major(true).build();
        ReflectionTestUtils.setField(station, "id", id);
        return station;
    }

    public static Train train(long id) {
        Train train = Train.builder().trainNumber("KTX 101").trainType(TrainType.KTX).build();
        ReflectionTestUtils.setField(train, "id", id);
        return train;
    }

    public static Schedule schedule() {
        Schedule schedule = Schedule.builder()
                .departureStation(station(1L, "서울"))
                .arrivalStation(station(2L, "부산"))
                .train(train(TRAIN_ID))
                .departureAt(DEPARTURE_AT)
                .arrivalAt(ARRIVAL_AT)
                .remark("서대구 정차")
                .build();
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);
        return schedule;
    }

    public static Coach coach(long id, Train train, int number, SeatClass seatClass) {
        Coach coach = Coach.builder().train(train).coachNumber(number).seatClass(seatClass).build();
        ReflectionTestUtils.setField(coach, "id", id);
        return coach;
    }

    public static Seat seat(long id, Coach coach, int row, String col) {
        Seat seat = Seat.builder()
                .coach(coach)
                .direction(Direction.FORWARD)
                .seatRow(row)
                .seatCol(col)
                .build();
        ReflectionTestUtils.setField(seat, "id", id);
        return seat;
    }

    public static Fare fare(int price) {
        return Fare.builder()
                .departureStation(station(1L, "서울"))
                .arrivalStation(station(2L, "부산"))
                .trainType(TrainType.KTX)
                .seatClass(SeatClass.STANDARD)
                .passengerType(PassengerType.ADULT)
                .price(price)
                .build();
    }

    public static Member member() {
        Member member = BeanUtils.instantiateClass(Member.class);
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    public static Reservation reservation(ReservationStatus status, LocalDateTime expiresAt) {
        Reservation reservation = Reservation.builder()
                .schedule(schedule())
                .member(member())
                .status(status)
                .totalPrice(119_600)
                .expiresAt(expiresAt)
                .build();
        ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);
        ReflectionTestUtils.setField(reservation, "createdAt", expiresAt.minusMinutes(10));
        return reservation;
    }
}
