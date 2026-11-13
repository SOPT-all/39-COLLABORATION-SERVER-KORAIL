package org.sopt.korail.schedule.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.korail.global.code.CommonErrorCode;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.schedule.domain.Fare;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.dto.response.ScheduleDetailResponse;
import org.sopt.korail.schedule.dto.response.ScheduleListResponse;
import org.sopt.korail.schedule.repository.FareRepository;
import org.sopt.korail.schedule.repository.ScheduleQueryRepository;
import org.sopt.korail.schedule.repository.ScheduleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sopt.korail.support.fixture.FareFixture.fare;
import static org.sopt.korail.support.fixture.ScheduleFixture.SCHEDULE_ID;
import static org.sopt.korail.support.fixture.ScheduleFixture.schedule;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 단위 테스트")
class ScheduleServiceTest {
    private static final String DEPARTURE = "서울";
    private static final String ARRIVAL = "부산";
    private static final LocalDate DATE = LocalDate.of(2026, 12, 1);

    @InjectMocks private ScheduleService scheduleService;

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleQueryRepository scheduleQueryRepository;
    @Mock private FareRepository fareRepository;

    private List<Fare> fares() {
        return List.of(
                fare(TrainType.KTX, SeatClass.FIRST, 100000),
                fare(TrainType.KTX, SeatClass.STANDARD, 60000),
                fare(TrainType.ITX, SeatClass.STANDARD, 40000));
    }

    @Nested
    @DisplayName("열차 목록 조회(getSchedules)")
    class GetSchedules {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("열차별 운임을 인원수만큼 곱하고 일반실, 특실 순으로 반환한다")
            void 조회성공_운임인원수반영() {
                Schedule schedule = schedule();
                given(scheduleQueryRepository.findSchedules(eq(DEPARTURE), eq(ARRIVAL), eq(DATE), isNull(), any(LocalDateTime.class)))
                        .willReturn(List.of(schedule));
                given(fareRepository.findFares(DEPARTURE, ARRIVAL, PassengerType.ADULT)).willReturn(fares());

                ScheduleListResponse response = scheduleService.getSchedules(DATE, 2, null);

                assertThat(response.passengerCount()).isEqualTo(2);
                assertThat(response.schedules()).hasSize(1);
                assertThat(response.schedules().get(0).scheduleId()).isEqualTo(SCHEDULE_ID);
                assertThat(response.schedules().get(0).durationMinutes()).isEqualTo(180);
                assertThat(response.schedules().get(0).fares()).hasSize(2);
                assertThat(response.schedules().get(0).fares().get(0).seatClass()).isEqualTo(SeatClass.STANDARD);
                assertThat(response.schedules().get(0).fares().get(0).totalPrice()).isEqualTo(120000);
                assertThat(response.schedules().get(0).fares().get(1).seatClass()).isEqualTo(SeatClass.FIRST);
                assertThat(response.schedules().get(0).fares().get(1).totalPrice()).isEqualTo(200000);
            }

            @Test
            @DisplayName("조건에 맞는 열차가 없으면 빈 목록을 반환한다")
            void 조회성공_결과없음_빈목록() {
                given(scheduleQueryRepository.findSchedules(eq(DEPARTURE), eq(ARRIVAL), eq(DATE), isNull(), any(LocalDateTime.class))).willReturn(List.of());
                given(fareRepository.findFares(DEPARTURE, ARRIVAL, PassengerType.ADULT)).willReturn(fares());

                ScheduleListResponse response = scheduleService.getSchedules(DATE, 1, null);

                assertThat(response.schedules()).isEmpty();
            }

            @Test
            @DisplayName("해당 열차 종류의 운임이 없으면 운임 목록이 비어 있다")
            void 조회성공_운임없음_빈운임() {
                given(scheduleQueryRepository.findSchedules(eq(DEPARTURE), eq(ARRIVAL), eq(DATE), isNull(), any(LocalDateTime.class)))
                        .willReturn(List.of(schedule()));
                given(fareRepository.findFares(DEPARTURE, ARRIVAL, PassengerType.ADULT)).willReturn(List.of());

                ScheduleListResponse response = scheduleService.getSchedules(DATE, 1, null);

                assertThat(response.schedules().get(0).fares()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("인원수가 0이면 BAD_REQUEST 예외가 발생한다")
            void 인원수0_BAD_REQUEST() {
                assertThatThrownBy(() -> scheduleService.getSchedules(DATE, 0, null))
                        .isInstanceOfSatisfying(GeneralException.class,
                                e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));

                verify(scheduleQueryRepository, never()).findSchedules(any(), any(), any(), any(), any());
            }

            @Test
            @DisplayName("인원수가 10이면 BAD_REQUEST 예외가 발생한다")
            void 인원수10_BAD_REQUEST() {
                assertThatThrownBy(() -> scheduleService.getSchedules(DATE, 10, null))
                        .isInstanceOfSatisfying(GeneralException.class,
                                e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));
            }
        }
    }

    @Nested
    @DisplayName("열차 상세 및 운임 조회(getSchedule)")
    class GetSchedule {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("스케줄 상세와 인원수를 반영한 운임을 반환한다")
            void 조회성공_상세와운임() {
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID)).willReturn(schedule());
                given(fareRepository.findFares(DEPARTURE, ARRIVAL, PassengerType.ADULT)).willReturn(fares());

                ScheduleDetailResponse response = scheduleService.getSchedule(SCHEDULE_ID, 3);

                assertThat(response.passengerCount()).isEqualTo(3);
                assertThat(response.scheduleId()).isEqualTo(SCHEDULE_ID);
                assertThat(response.trainType()).isEqualTo(TrainType.KTX);
                assertThat(response.fares()).hasSize(2);
                assertThat(response.fares().get(0).unitPrice()).isEqualTo(60000);
                assertThat(response.fares().get(0).totalPrice()).isEqualTo(180000);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 스케줄이면 SCHEDULE_NOT_FOUND 예외가 발생한다")
            void 스케줄없음_SCHEDULE_NOT_FOUND() {
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID))
                        .willThrow(new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

                assertThatThrownBy(() -> scheduleService.getSchedule(SCHEDULE_ID, 1))
                        .isInstanceOfSatisfying(GeneralException.class,
                                e -> assertThat(e.getErrorCode()).isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
            }

            @Test
            @DisplayName("인원수가 범위를 벗어나면 BAD_REQUEST 예외가 발생한다")
            void 인원수범위초과_BAD_REQUEST() {
                assertThatThrownBy(() -> scheduleService.getSchedule(SCHEDULE_ID, 10))
                        .isInstanceOfSatisfying(GeneralException.class,
                                e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST));

                verify(scheduleRepository, never()).getByIdOrThrow(any());
            }
        }
    }
}
