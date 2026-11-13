package org.sopt.korail.seat.repository;

import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.dto.CoachSeatSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CoachRepository extends JpaRepository<Coach, Long> {
    // 호차별 전체 좌석 수와 점유 좌석 수
    // TODO: 해민 티켓 예약 구현 이후 만료 조건 확인
    @Query("""
            select new org.sopt.korail.seat.dto.CoachSeatSummary(
                   c.id,
                   c.coachNumber,
                   (select count(s)
                      from Seat s
                     where s.coach = c),
                   (select count(distinct t.seat.id)
                      from Ticket t
                      join t.reservation r
                     where t.seat.coach = c
                       and r.schedule = :schedule
                       and t.returnedAt is null
                       and (r.status <> org.sopt.korail.reservation.domain.ReservationStatus.PENDING
                            or r.expiresAt > :now)))
            from Coach c
            where c.train = (select sc.train from Schedule sc where sc = :schedule)
              and c.seatClass = :seatClass
            order by c.coachNumber
            """)
    List<CoachSeatSummary> findCoachSeatSummaries(
            @Param("schedule") Schedule schedule,
            @Param("seatClass") SeatClass seatClass,
            @Param("now") LocalDateTime now
    );
}
