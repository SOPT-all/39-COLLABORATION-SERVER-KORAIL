package org.sopt.korail.seat.repository;

import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.schedule.domain.Train;
import org.sopt.korail.seat.domain.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoachRepository extends JpaRepository<Coach, Long> {
    // 열차에 속한 호차만 조회
    Optional<Coach> findByIdAndTrain(Long id, Train train);

    // 열차의 좌석 등급별 호차를 호차 번호 순으로 조회
    @Query("""
            select c
            from Coach c
            where c.train = :train
              and c.seatClass = :seatClass
            order by c.coachNumber
            """)
    List<Coach> findCoachesByTrainAndSeatClass(
            @Param("train") Train train,
            @Param("seatClass") SeatClass seatClass
    );
}
