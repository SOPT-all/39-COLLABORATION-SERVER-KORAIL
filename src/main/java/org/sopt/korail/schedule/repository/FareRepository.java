package org.sopt.korail.schedule.repository;

import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Fare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FareRepository extends JpaRepository<Fare, Long> {
    Optional<Fare> findByDepartureStationIdAndArrivalStationIdAndTrainTypeAndSeatClassAndPassengerType(
            Long departureStationId,
            Long arrivalStationId,
            TrainType trainType,
            SeatClass seatClass,
            PassengerType passengerType
    );

    // 구간의 모든 열차 종류·좌석 등급 운임을 한 번에 조회한다. 일정마다 조회하면 N+1이 생긴다.
    @Query("""
            select f from Fare f
            where f.departureStation.name = :departureStationName
              and f.arrivalStation.name = :arrivalStationName
              and f.passengerType = :passengerType
            """)
    List<Fare> findFares(
            @Param("departureStationName") String departureStationName,
            @Param("arrivalStationName") String arrivalStationName,
            @Param("passengerType") PassengerType passengerType
    );
}
