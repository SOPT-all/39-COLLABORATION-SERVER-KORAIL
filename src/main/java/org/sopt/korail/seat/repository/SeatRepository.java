package org.sopt.korail.seat.repository;

import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("""
            select s
            from Seat s
            where s.coach = :coach
            order by s.seatRow, s.seatCol
            """)
    List<Seat> findSeats(@Param("coach") Coach coach);
}
