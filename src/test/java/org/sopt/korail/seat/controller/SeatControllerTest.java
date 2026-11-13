package org.sopt.korail.seat.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.code.CommonErrorCode;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.seat.code.SeatSuccessCode;
import org.sopt.korail.seat.dto.response.CoachListResponse;
import org.sopt.korail.seat.dto.response.CoachResponse;
import org.sopt.korail.seat.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.sopt.korail.support.fixture.ScheduleFixture.SCHEDULE_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatController.class)
@DisplayName("SeatController 테스트")
class SeatControllerTest {

    private static final String COACHES_URL = "/api/v1/schedules/{scheduleId}/coaches";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeatService seatService;

    @Nested
    @DisplayName("GET /api/v1/schedules/{scheduleId}/coaches - 호차 목록 조회")
    class GetCoaches {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("스케줄과 좌석 등급을 지정하면 호차 목록을 반환한다")
            void 조회성공_호차목록반환() throws Exception {
                CoachResponse coach = CoachResponse.builder()
                        .coachId(10L)
                        .coachNumber(1)
                        .remainingSeatCount(17)
                        .totalSeatCount(48)
                        .reservable(true)
                        .build();
                given(seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD))
                        .willReturn(CoachListResponse.of(SCHEDULE_ID, SeatClass.STANDARD, List.of(coach)));

                mockMvc.perform(get(COACHES_URL, SCHEDULE_ID).param("seatClass", "STANDARD"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(SeatSuccessCode.COACH_LIST_OK.getCode()))
                        .andExpect(jsonPath("$.result.scheduleId").value(SCHEDULE_ID))
                        .andExpect(jsonPath("$.result.seatClass").value("STANDARD"))
                        .andExpect(jsonPath("$.result.coaches.length()").value(1))
                        .andExpect(jsonPath("$.result.coaches[0].coachId").value(10L))
                        .andExpect(jsonPath("$.result.coaches[0].coachNumber").value(1))
                        .andExpect(jsonPath("$.result.coaches[0].remainingSeatCount").value(17))
                        .andExpect(jsonPath("$.result.coaches[0].totalSeatCount").value(48))
                        .andExpect(jsonPath("$.result.coaches[0].reservable").value(true));
            }

            @Test
            @DisplayName("좌석 등급을 생략하면 STANDARD로 조회한다")
            void 좌석등급생략_STANDARD로조회() throws Exception {
                given(seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD))
                        .willReturn(CoachListResponse.of(SCHEDULE_ID, SeatClass.STANDARD, List.of()));

                mockMvc.perform(get(COACHES_URL, SCHEDULE_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result.seatClass").value("STANDARD"));

                verify(seatService).getCoaches(SCHEDULE_ID, SeatClass.STANDARD);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("스케줄이 없으면 404와 SCHEDULE_NOT_FOUND 코드를 반환한다")
            void 스케줄없음_404를_반환한다() throws Exception {
                given(seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD))
                        .willThrow(new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

                mockMvc.perform(get(COACHES_URL, SCHEDULE_ID).param("seatClass", "STANDARD"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ScheduleErrorCode.SCHEDULE_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("좌석 등급 값이 올바르지 않으면 400을 반환한다")
            void 잘못된좌석등급_400을_반환한다() throws Exception {
                mockMvc.perform(get(COACHES_URL, SCHEDULE_ID).param("seatClass", "PREMIUM"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCode()));
            }
        }
    }
}
