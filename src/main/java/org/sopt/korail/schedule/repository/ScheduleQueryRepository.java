package org.sopt.korail.schedule.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.QStation;
import org.sopt.korail.schedule.domain.Schedule;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static org.sopt.korail.schedule.domain.QSchedule.schedule;
import static org.sopt.korail.schedule.domain.QTrain.train;

@Repository
@RequiredArgsConstructor
public class ScheduleQueryRepository {
    private static final QStation departureStation = new QStation("departureStation");
    private static final QStation arrivalStation = new QStation("arrivalStation");

    private final JPAQueryFactory queryFactory;

    // 출발일과 열차 종류는 선택 조건이다. null이거나 비어 있으면 조건에서 제외한다.
    public List<Schedule> findSchedules(
            String departureStationName,
            String arrivalStationName,
            LocalDate departureDate,
            List<TrainType> trainTypes
    ) {
        return queryFactory
                .selectFrom(schedule)
                .join(schedule.train, train).fetchJoin()
                .join(schedule.departureStation, departureStation)
                .join(schedule.arrivalStation, arrivalStation)
                .where(
                        departureStation.name.eq(departureStationName),
                        arrivalStation.name.eq(arrivalStationName),
                        departureDateEq(departureDate),
                        trainTypeIn(trainTypes)
                )
                .orderBy(schedule.departureAt.asc())
                .fetch();
    }

    private BooleanExpression departureDateEq(LocalDate departureDate) {
        if (departureDate == null) {
            return null;
        }
        return schedule.departureAt.goe(departureDate.atStartOfDay())
                .and(schedule.departureAt.lt(departureDate.plusDays(1).atStartOfDay()));
    }

    private BooleanExpression trainTypeIn(List<TrainType> trainTypes) {
        if (trainTypes == null || trainTypes.isEmpty()) {
            return null;
        }
        return train.trainType.in(trainTypes);
    }
}
