package org.sopt.korail.schedule.repository;

import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.StationErrorCode;
import org.sopt.korail.schedule.domain.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {

    boolean existsByName(String name);

    default void validateExists(String name) {
        if (!existsByName(name)) {
            throw new GeneralException(StationErrorCode.STATION_NOT_FOUND);
        }
    }
}
