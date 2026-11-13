package org.sopt.korail.schedule.service;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.CommonErrorCode;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.domain.Fare;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.dto.response.FareResponse;
import org.sopt.korail.schedule.dto.response.ScheduleDetailResponse;
import org.sopt.korail.schedule.dto.response.ScheduleListResponse;
import org.sopt.korail.schedule.dto.response.ScheduleResponse;
import org.sopt.korail.schedule.repository.FareRepository;
import org.sopt.korail.schedule.repository.ScheduleQueryRepository;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
    private static final String DEPARTURE_STATION_NAME = "서울";
    private static final String ARRIVAL_STATION_NAME = "부산";
    private static final PassengerType PASSENGER_TYPE = PassengerType.ADULT;
    private static final int MIN_PASSENGER_COUNT = 1;
    private static final int MAX_PASSENGER_COUNT = 9;

    private final ScheduleRepository scheduleRepository;
    private final ScheduleQueryRepository scheduleQueryRepository;
    private final FareRepository fareRepository;

    public ScheduleListResponse getSchedules(LocalDate departureDate, int passengerCount, List<TrainType> trainTypes) {
        validatePassengerCount(passengerCount);

        List<Schedule> schedules = scheduleQueryRepository.findSchedules(
                DEPARTURE_STATION_NAME, ARRIVAL_STATION_NAME, departureDate, trainTypes);
        Map<TrainType, List<Fare>> faresByTrainType = findFaresByTrainType();

        List<ScheduleResponse> responses = schedules.stream()
                .map(schedule -> toResponse(schedule, faresByTrainType, passengerCount))
                .toList();
        return ScheduleListResponse.of(passengerCount, responses);
    }

    public ScheduleDetailResponse getSchedule(Long scheduleId, int passengerCount) {
        validatePassengerCount(passengerCount);

        Schedule schedule = scheduleRepository.getByIdOrThrow(scheduleId);
        ScheduleResponse response = toResponse(schedule, findFaresByTrainType(), passengerCount);
        return ScheduleDetailResponse.of(passengerCount, response);
    }

    private void validatePassengerCount(int passengerCount) {
        if (passengerCount < MIN_PASSENGER_COUNT || passengerCount > MAX_PASSENGER_COUNT) {
            throw new GeneralException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private Map<TrainType, List<Fare>> findFaresByTrainType() {
        return fareRepository.findFares(DEPARTURE_STATION_NAME, ARRIVAL_STATION_NAME, PASSENGER_TYPE).stream()
                .collect(Collectors.groupingBy(Fare::getTrainType));
    }

    private ScheduleResponse toResponse(Schedule schedule, Map<TrainType, List<Fare>> faresByTrainType, int passengerCount) {
        List<FareResponse> fares = faresByTrainType
                .getOrDefault(schedule.getTrain().getTrainType(), List.of()).stream()
                .sorted(Comparator.comparing(Fare::getSeatClass))
                .map(fare -> FareResponse.of(fare.getSeatClass(), fare.getPrice(), passengerCount))
                .toList();
        return ScheduleResponse.of(schedule, fares);
    }
}
