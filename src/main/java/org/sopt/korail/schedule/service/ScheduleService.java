package org.sopt.korail.schedule.service;

import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Fare;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.dto.response.FareResponse;
import org.sopt.korail.schedule.dto.response.ScheduleDetailResponse;
import org.sopt.korail.schedule.dto.response.ScheduleListResponse;
import org.sopt.korail.schedule.dto.response.ScheduleResponse;
import org.sopt.korail.schedule.repository.FareRepository;
import org.sopt.korail.schedule.repository.ScheduleQueryRepository;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.sopt.korail.schedule.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
    private static final PassengerType PASSENGER_TYPE = PassengerType.ADULT;

    private final ScheduleRepository scheduleRepository;
    private final ScheduleQueryRepository scheduleQueryRepository;
    private final FareRepository fareRepository;
    private final StationRepository stationRepository;
    private final Clock clock;

    public ScheduleListResponse getSchedules(
            String departureStationName,
            String arrivalStationName,
            LocalDate departureDate,
            int passengerCount,
            List<TrainType> trainTypes
    ) {
        // 존재하지 않는 역은 빈 목록과 구분할 수 있도록 예외로 알린다.
        stationRepository.validateExists(departureStationName);
        stationRepository.validateExists(arrivalStationName);

        List<Schedule> schedules = scheduleQueryRepository.findSchedules(
                departureStationName, arrivalStationName, departureDate, trainTypes, LocalDateTime.now(clock));
        Map<TrainType, List<Fare>> faresByTrainType = findFaresByTrainType(departureStationName, arrivalStationName);

        List<ScheduleResponse> responses = schedules.stream()
                .map(schedule -> toResponse(schedule, faresByTrainType, passengerCount))
                .toList();
        return ScheduleListResponse.of(passengerCount, responses);
    }

    public ScheduleDetailResponse getSchedule(Long scheduleId, int passengerCount) {
        Schedule schedule = scheduleRepository.getByIdOrThrow(scheduleId);
        // 운임은 열차가 실제로 운행하는 구간 기준으로 조회한다.
        Map<TrainType, List<Fare>> faresByTrainType = findFaresByTrainType(
                schedule.getDepartureStation().getName(), schedule.getArrivalStation().getName());
        ScheduleResponse response = toResponse(schedule, faresByTrainType, passengerCount);
        return ScheduleDetailResponse.of(passengerCount, response);
    }

    private Map<TrainType, List<Fare>> findFaresByTrainType(String departureStationName, String arrivalStationName) {
        return fareRepository.findSectionFares(departureStationName, arrivalStationName, PASSENGER_TYPE).stream()
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
