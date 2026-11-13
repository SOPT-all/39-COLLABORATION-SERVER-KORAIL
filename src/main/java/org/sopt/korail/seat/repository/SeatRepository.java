package org.sopt.korail.seat.repository;

import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.schedule.domain.Train;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Seat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @EntityGraph(attributePaths = "coach")
    @Query("""
            select s
            from Seat s
            where s.coach.train = :train
              and s.coach.seatClass = :seatClass
            order by s.coach.coachNumber, s.seatRow, s.seatCol
            """)
    List<Seat> findSeatsByTrainAndSeatClass(
            @Param("train") Train train,
            @Param("seatClass") SeatClass seatClass
    );

    @EntityGraph(attributePaths = "coach")
    List<Seat> findAllByIdIn(List<Long> ids);

    @Query("""
            select s
            from Seat s
            where s.coach = :coach
            order by s.seatRow, s.seatCol
            """)
    List<Seat> findSeatsByCoach(@Param("coach") Coach coach);
}
