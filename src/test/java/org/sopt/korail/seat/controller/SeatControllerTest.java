package org.sopt.korail.seat.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.code.CommonErrorCode;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.seat.code.SeatErrorCode;
import org.sopt.korail.seat.code.SeatSuccessCode;
import org.sopt.korail.seat.domain.Direction;
import org.sopt.korail.seat.dto.response.CoachListResponse;
import org.sopt.korail.seat.dto.response.CoachResponse;
import org.sopt.korail.seat.dto.response.SeatLayoutResponse;
import org.sopt.korail.seat.dto.response.SeatResponse;
import org.sopt.korail.seat.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.sopt.korail.support.fixture.ScheduleFixture.SCHEDULE_ID;
import static org.sopt.korail.support.fixture.SeatFixture.COACH_ID;
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
    @DisplayName("GET /api/v1/schedules/{scheduleId}/coaches/{coachId}/seats - 좌석 배치 조회")
    class GetSeats {

        private static final String SEATS_URL = COACHES_URL + "/{coachId}/seats";

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("스케줄과 호차를 지정하면 좌석 배치를 반환한다")
            void 조회성공_좌석배치반환() throws Exception {
                SeatLayoutResponse response = SeatLayoutResponse.builder()
                        .scheduleId(SCHEDULE_ID)
                        .coachId(COACH_ID)
                        .coachNumber(1)
                        .seatClass(SeatClass.STANDARD)
                        .remainingSeatCount(1)
                        .totalSeatCount(2)
                        .seats(List.of(
                                new SeatResponse(101L, "1A", 1, "A", Direction.FORWARD, false),
                                new SeatResponse(102L, "1B", 1, "B", Direction.FORWARD, true)))
                        .build();
                given(seatService.getSeats(SCHEDULE_ID, COACH_ID)).willReturn(response);

                mockMvc.perform(get(SEATS_URL, SCHEDULE_ID, COACH_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(SeatSuccessCode.SEAT_LAYOUT_OK.getCode()))
                        .andExpect(jsonPath("$.result.scheduleId").value(SCHEDULE_ID))
                        .andExpect(jsonPath("$.result.coachId").value(COACH_ID))
                        .andExpect(jsonPath("$.result.coachNumber").value(1))
                        .andExpect(jsonPath("$.result.seatClass").value("STANDARD"))
                        .andExpect(jsonPath("$.result.remainingSeatCount").value(1))
                        .andExpect(jsonPath("$.result.totalSeatCount").value(2))
                        .andExpect(jsonPath("$.result.seats.length()").value(2))
                        .andExpect(jsonPath("$.result.seats[0].seatId").value(101L))
                        .andExpect(jsonPath("$.result.seats[0].seatNumber").value("1A"))
                        .andExpect(jsonPath("$.result.seats[0].row").value(1))
                        .andExpect(jsonPath("$.result.seats[0].column").value("A"))
                        .andExpect(jsonPath("$.result.seats[0].direction").value("FORWARD"))
                        .andExpect(jsonPath("$.result.seats[0].available").value(false))
                        .andExpect(jsonPath("$.result.seats[1].available").value(true));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("스케줄이 없으면 404와 SCHEDULE_NOT_FOUND 코드를 반환한다")
            void 좌석조회시_스케줄없음_404를_반환한다() throws Exception {
                given(seatService.getSeats(SCHEDULE_ID, COACH_ID))
                        .willThrow(new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

                mockMvc.perform(get(SEATS_URL, SCHEDULE_ID, COACH_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ScheduleErrorCode.SCHEDULE_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("호차가 없으면 404와 COACH_NOT_FOUND 코드를 반환한다")
            void 호차없음_404를_반환한다() throws Exception {
                given(seatService.getSeats(SCHEDULE_ID, COACH_ID))
                        .willThrow(new GeneralException(SeatErrorCode.COACH_NOT_FOUND));

                mockMvc.perform(get(SEATS_URL, SCHEDULE_ID, COACH_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(SeatErrorCode.COACH_NOT_FOUND.getCode()));
            }
        }
    }

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
