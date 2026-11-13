package org.sopt.korail.support.fixture;

import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.reservation.domain.Reservation;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.reservation.domain.Ticket;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.seat.domain.Seat;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.sopt.korail.support.fixture.MemberFixture.member;

public final class ReservationFixture {
    public static final Long RESERVATION_ID = 10L;
    public static final Long TICKET_ID = 100L;

    private ReservationFixture() {
    }

    public static Reservation reservation(Schedule schedule, ReservationStatus status, LocalDateTime expiresAt) {
        Reservation reservation = Reservation.builder()
                .schedule(schedule)
                .member(member())
                .status(status)
                .totalPrice(119_600)
                .expiresAt(expiresAt)
                .build();
        ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);
        return reservation;
    }

    public static Ticket ticket(Reservation reservation, Seat seat, int price) {
        Ticket ticket = Ticket.builder()
                .reservation(reservation)
                .seat(seat)
                .passengerType(PassengerType.ADULT)
                .price(price)
                .build();
        ReflectionTestUtils.setField(ticket, "id", TICKET_ID);
        return ticket;
    }
}
