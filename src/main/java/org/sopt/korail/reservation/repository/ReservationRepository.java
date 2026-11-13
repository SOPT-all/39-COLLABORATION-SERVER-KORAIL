package org.sopt.korail.reservation.repository;

import org.sopt.korail.reservation.domain.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {
            "schedule", "schedule.train", "schedule.departureStation", "schedule.arrivalStation"
    })
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findDetailById(@Param("id") Long id);
}
