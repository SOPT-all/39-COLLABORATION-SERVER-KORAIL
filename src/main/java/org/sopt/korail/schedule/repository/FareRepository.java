package org.sopt.korail.schedule.repository;

import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Fare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FareRepository extends JpaRepository<Fare, Long> {
    Optional<Fare> findByDepartureStationIdAndArrivalStationIdAndTrainTypeAndSeatClassAndPassengerType(
            Long departureStationId,
            Long arrivalStationId,
            TrainType trainType,
            SeatClass seatClass,
            PassengerType passengerType
    );
}
