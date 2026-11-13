package org.sopt.korail.reservation.repository;

import org.sopt.korail.reservation.domain.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
            select t.seat.id
            from Ticket t
            where t.reservation.schedule.id = :scheduleId
              and t.returnedAt is null
              and (t.reservation.status = org.sopt.korail.reservation.domain.ReservationStatus.PAID
                   or (t.reservation.status = org.sopt.korail.reservation.domain.ReservationStatus.PENDING
                       and t.reservation.expiresAt > :now))
            """)
    List<Long> findOccupiedSeatIds(@Param("scheduleId") Long scheduleId, @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"seat", "seat.coach"})
    @Query("""
            select t
            from Ticket t
            where t.reservation.id = :reservationId
              and t.returnedAt is null
            order by t.seat.coach.coachNumber, t.seat.seatRow, t.seat.seatCol
            """)
    List<Ticket> findActiveTicketsByReservationId(@Param("reservationId") Long reservationId);
}
