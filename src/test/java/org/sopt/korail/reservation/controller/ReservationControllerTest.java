package org.sopt.korail.reservation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.code.CommonErrorCode;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.reservation.code.ReservationErrorCode;
import org.sopt.korail.reservation.code.ReservationSuccessCode;
import org.sopt.korail.reservation.domain.ReservationStatus;
import org.sopt.korail.reservation.dto.CreateReservationRequest;
import org.sopt.korail.reservation.dto.CreateReservationResponse;
import org.sopt.korail.reservation.dto.ReservationDetailResponse;
import org.sopt.korail.reservation.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.sopt.korail.support.fixture.ReservationFixture.RESERVATION_ID;
import static org.sopt.korail.support.fixture.ReservationFixture.SCHEDULE_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@DisplayName("ReservationController 테스트")
class ReservationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ReservationService reservationService;

    @Nested
    @DisplayName("POST /api/v1/reservations - 승차권 예약 생성")
    class CreateReservation {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("유효한 요청이면 201과 예약 ID를 반환한다")
            void 유효한요청_201을_반환한다() throws Exception {
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 2, List.of(101L, 102L));
                given(reservationService.createReservation(any(CreateReservationRequest.class)))
                        .willReturn(new CreateReservationResponse(RESERVATION_ID));

                mockMvc.perform(post("/api/v1/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(ReservationSuccessCode.CREATE_RESERVATION.getCode()))
                        .andExpect(jsonPath("$.result.reservationId").value(RESERVATION_ID));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("인원 수가 1보다 작으면 COMMON400을 반환한다")
            void 잘못된인원수_COMMON400을_반환한다() throws Exception {
                CreateReservationRequest request = new CreateReservationRequest(
                        SCHEDULE_ID, SeatClass.STANDARD, 0, null);

                mockMvc.perform(post("/api/v1/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reservations/{reservationId} - 승차권 예약 조회")
    class GetReservation {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("예약이 존재하면 200과 예약 정보를 반환한다")
            void 존재하는예약_200을_반환한다() throws Exception {
                LocalDateTime now = LocalDateTime.of(2026, 11, 11, 20, 0);
                ReservationDetailResponse response = new ReservationDetailResponse(
                        RESERVATION_ID,
                        ReservationStatus.PENDING,
                        119_600,
                        now.plusMinutes(10),
                        now,
                        null,
                        List.of()
                );
                given(reservationService.getReservation(RESERVATION_ID)).willReturn(response);

                mockMvc.perform(get("/api/v1/reservations/{reservationId}", RESERVATION_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(ReservationSuccessCode.GET_RESERVATION.getCode()))
                        .andExpect(jsonPath("$.result.reservationId").value(RESERVATION_ID))
                        .andExpect(jsonPath("$.result.status").value("PENDING"))
                        .andExpect(jsonPath("$.result.totalPrice").value(119_600));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("예약이 없으면 RESERVATION4041을 반환한다")
            void 없는예약_RESERVATION4041을_반환한다() throws Exception {
                given(reservationService.getReservation(RESERVATION_ID))
                        .willThrow(new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND));

                mockMvc.perform(get("/api/v1/reservations/{reservationId}", RESERVATION_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ReservationErrorCode.RESERVATION_NOT_FOUND.getCode()));
            }
        }
    }
}
