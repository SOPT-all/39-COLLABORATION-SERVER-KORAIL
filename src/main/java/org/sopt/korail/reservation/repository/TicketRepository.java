package org.sopt.korail.reservation.repository;

import org.sopt.korail.reservation.domain.Ticket;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.seat.domain.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // 호차에서 점유된 좌석 ID
    // TODO: 해민 티켓 예약 구현 이후 CoachRepository.findCoachSeatSummaries 수정 및 한 곳으로 모으기
    @Query("""
            select distinct t.seat.id
            from Ticket t
            join t.reservation r
            where t.seat.coach = :coach
              and r.schedule = :schedule
              and t.returnedAt is null
              and (r.status <> org.sopt.korail.reservation.domain.ReservationStatus.PENDING
                   or r.expiresAt > :now)
            """)
    List<Long> findOccupiedSeatIds(
            @Param("schedule") Schedule schedule,
            @Param("coach") Coach coach,
            @Param("now") LocalDateTime now
    );
}
