package org.sopt.korail.schedule.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.code.CommonErrorCode;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.schedule.code.ScheduleSuccessCode;
import org.sopt.korail.schedule.dto.response.FareResponse;
import org.sopt.korail.schedule.dto.response.ScheduleDetailResponse;
import org.sopt.korail.schedule.dto.response.ScheduleListResponse;
import org.sopt.korail.schedule.dto.response.ScheduleResponse;
import org.sopt.korail.schedule.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.sopt.korail.support.fixture.ScheduleFixture.SCHEDULE_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
@DisplayName("ScheduleController 테스트")
class ScheduleControllerTest {

    private static final String SCHEDULES_URL = "/api/v1/schedules";
    private static final LocalDate DATE = LocalDate.of(2026, 12, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    private ScheduleResponse scheduleResponse() {
        LocalDateTime departureAt = LocalDateTime.of(2026, 12, 1, 9, 0);
        return new ScheduleResponse(
                SCHEDULE_ID, "KTX1", TrainType.KTX, "KTX",
                departureAt, departureAt.plusHours(3), 180, null,
                List.of(FareResponse.of(SeatClass.STANDARD, 60000, 2)));
    }

    @Nested
    @DisplayName("GET /api/v1/schedules - 열차 목록 조회")
    class GetSchedules {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("조건을 지정하면 열차 목록을 반환한다")
            void 조회성공_목록반환() throws Exception {
                given(scheduleService.getSchedules(DATE, 2, List.of(TrainType.KTX, TrainType.ITX)))
                        .willReturn(ScheduleListResponse.of(2, List.of(scheduleResponse())));

                mockMvc.perform(get(SCHEDULES_URL)
                                .param("departureDate", "2026-12-01")
                                .param("passengerCount", "2")
                                .param("trainTypes", "KTX,ITX"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(ScheduleSuccessCode.SCHEDULE_LIST_OK.getCode()))
                        .andExpect(jsonPath("$.result.passengerCount").value(2))
                        .andExpect(jsonPath("$.result.schedules[0].scheduleId").value(SCHEDULE_ID))
                        .andExpect(jsonPath("$.result.schedules[0].durationMinutes").value(180))
                        .andExpect(jsonPath("$.result.schedules[0].fares[0].totalPrice").value(120000));
            }

            @Test
            @DisplayName("파라미터를 생략하면 인원수 1과 전체 조건으로 조회한다")
            void 조회성공_기본값() throws Exception {
                given(scheduleService.getSchedules(null, 1, null))
                        .willReturn(ScheduleListResponse.of(1, List.of()));

                mockMvc.perform(get(SCHEDULES_URL))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result.passengerCount").value(1));

                verify(scheduleService).getSchedules(null, 1, null);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("인원수가 범위를 벗어나면 400을 반환한다")
            void 인원수오류_400() throws Exception {
                given(scheduleService.getSchedules(null, 10, null))
                        .willThrow(new GeneralException(CommonErrorCode.BAD_REQUEST));

                mockMvc.perform(get(SCHEDULES_URL).param("passengerCount", "10"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCode()));
            }

            @Test
            @DisplayName("열차 종류가 올바르지 않으면 400을 반환한다")
            void 열차종류오류_400() throws Exception {
                mockMvc.perform(get(SCHEDULES_URL).param("trainTypes", "UNKNOWN"))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/schedules/{scheduleId} - 열차 상세 및 운임 조회")
    class GetSchedule {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("스케줄 ID를 지정하면 상세와 운임을 반환한다")
            void 조회성공_상세반환() throws Exception {
                given(scheduleService.getSchedule(SCHEDULE_ID, 2))
                        .willReturn(ScheduleDetailResponse.of(2, scheduleResponse()));

                mockMvc.perform(get(SCHEDULES_URL + "/{scheduleId}", SCHEDULE_ID).param("passengerCount", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(ScheduleSuccessCode.SCHEDULE_DETAIL_OK.getCode()))
                        .andExpect(jsonPath("$.result.passengerCount").value(2))
                        .andExpect(jsonPath("$.result.scheduleId").value(SCHEDULE_ID))
                        .andExpect(jsonPath("$.result.trainTypeName").value("KTX"))
                        .andExpect(jsonPath("$.result.fares[0].unitPrice").value(60000));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 스케줄이면 404를 반환한다")
            void 스케줄없음_404() throws Exception {
                given(scheduleService.getSchedule(SCHEDULE_ID, 1))
                        .willThrow(new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

                mockMvc.perform(get(SCHEDULES_URL + "/{scheduleId}", SCHEDULE_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ScheduleErrorCode.SCHEDULE_NOT_FOUND.getCode()));
            }
        }
    }
}
