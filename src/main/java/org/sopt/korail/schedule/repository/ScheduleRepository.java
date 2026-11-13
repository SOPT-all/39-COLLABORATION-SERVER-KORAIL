package org.sopt.korail.schedule.repository;

import jakarta.persistence.LockModeType;
import org.sopt.korail.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Schedule s
            join fetch s.train
            join fetch s.departureStation
            join fetch s.arrivalStation
            where s.id = :id
            """)
    Optional<Schedule> findByIdForUpdate(@Param("id") Long id);
}
