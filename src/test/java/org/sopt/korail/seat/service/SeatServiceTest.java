package org.sopt.korail.seat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.schedule.domain.Schedule;
import org.sopt.korail.schedule.repository.ScheduleRepository;
import org.sopt.korail.reservation.repository.TicketRepository;
import org.sopt.korail.seat.code.SeatErrorCode;
import org.sopt.korail.seat.domain.Coach;
import org.sopt.korail.seat.domain.Direction;
import org.sopt.korail.seat.domain.Seat;
import org.sopt.korail.seat.dto.CoachSeatSummary;
import org.sopt.korail.seat.dto.response.CoachListResponse;
import org.sopt.korail.seat.dto.response.CoachResponse;
import org.sopt.korail.seat.dto.response.SeatLayoutResponse;
import org.sopt.korail.seat.dto.response.SeatResponse;
import org.sopt.korail.seat.repository.CoachRepository;
import org.sopt.korail.seat.repository.SeatRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sopt.korail.support.fixture.ScheduleFixture.SCHEDULE_ID;
import static org.sopt.korail.support.fixture.ScheduleFixture.schedule;
import static org.sopt.korail.support.fixture.SeatFixture.COACH_ID;
import static org.sopt.korail.support.fixture.SeatFixture.coach;
import static org.sopt.korail.support.fixture.SeatFixture.seat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatService 단위 테스트")
class SeatServiceTest {
    @InjectMocks private SeatService seatService;

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CoachRepository coachRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private TicketRepository ticketRepository;

    @Nested
    @DisplayName("좌석 배치 조회(getSeats)")
    class GetSeats {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("점유된 좌석은 선택 불가, 나머지 좌석은 선택 가능으로 반환한다")
            void 점유좌석_선택불가반환() {
                Schedule schedule = schedule();
                Coach coach = coach(schedule.getTrain());
                Seat occupied = seat(coach, 101L, 1, "A");
                Seat empty = seat(coach, 102L, 1, "B");
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID)).willReturn(schedule);
                given(coachRepository.findByIdAndTrain(COACH_ID, schedule.getTrain())).willReturn(Optional.of(coach));
                given(seatRepository.findSeats(coach)).willReturn(List.of(occupied, empty));
                given(ticketRepository.findOccupiedSeatIds(eq(schedule), eq(coach), any(LocalDateTime.class)))
                        .willReturn(List.of(101L));

                SeatLayoutResponse result = seatService.getSeats(SCHEDULE_ID, COACH_ID);

                assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
                assertThat(result.coachId()).isEqualTo(COACH_ID);
                assertThat(result.coachNumber()).isEqualTo(1);
                assertThat(result.seatClass()).isEqualTo(SeatClass.STANDARD);
                assertThat(result.totalSeatCount()).isEqualTo(2);
                assertThat(result.remainingSeatCount()).isEqualTo(1);
                assertThat(result.seats()).containsExactly(
                        new SeatResponse(101L, "1A", 1, "A", Direction.FORWARD, false),
                        new SeatResponse(102L, "1B", 1, "B", Direction.FORWARD, true)
                );
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("스케줄이 없으면 SCHEDULE_NOT_FOUND를 던진다")
            void 좌석조회시_스케줄없음_예외() {
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID))
                        .willThrow(new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

                assertThatThrownBy(() -> seatService.getSeats(SCHEDULE_ID, COACH_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
            }

            @Test
            @DisplayName("스케줄 열차에 해당 호차가 없으면 COACH_NOT_FOUND를 던진다")
            void 호차없음_예외() {
                Schedule schedule = schedule();
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID)).willReturn(schedule);
                given(coachRepository.findByIdAndTrain(COACH_ID, schedule.getTrain())).willReturn(Optional.empty());

                assertThatThrownBy(() -> seatService.getSeats(SCHEDULE_ID, COACH_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(SeatErrorCode.COACH_NOT_FOUND));
                verify(seatRepository, never()).findSeats(any());
            }
        }
    }

    @Nested
    @DisplayName("호차 목록 조회(getCoaches)")
    class GetCoaches {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("전체 좌석 수에서 점유 좌석 수를 빼서 잔여 좌석 수를 반환한다")
            void 집계결과_잔여석계산() {
                Schedule schedule = schedule();
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID)).willReturn(schedule);
                given(coachRepository.findCoachSeatSummaries(eq(schedule), eq(SeatClass.STANDARD), any(LocalDateTime.class)))
                        .willReturn(List.of(new CoachSeatSummary(10L, 1, 48, 31)));

                CoachListResponse result = seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD);

                assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
                assertThat(result.seatClass()).isEqualTo(SeatClass.STANDARD);
                assertThat(result.coaches()).containsExactly(CoachResponse.builder()
                        .coachId(10L)
                        .coachNumber(1)
                        .remainingSeatCount(17)
                        .totalSeatCount(48)
                        .reservable(true)
                        .build());
            }

            @Test
            @DisplayName("잔여 좌석이 없으면 선택 불가로 반환한다")
            void 잔여석없음_선택불가() {
                Schedule schedule = schedule();
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID)).willReturn(schedule);
                given(coachRepository.findCoachSeatSummaries(eq(schedule), eq(SeatClass.STANDARD), any(LocalDateTime.class)))
                        .willReturn(List.of(new CoachSeatSummary(10L, 3, 48, 48)));

                CoachListResponse result = seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD);

                assertThat(result.coaches()).singleElement()
                        .satisfies(coach -> {
                            assertThat(coach.remainingSeatCount()).isZero();
                            assertThat(coach.reservable()).isFalse();
                        });
            }

            @Test
            @DisplayName("해당 좌석 등급의 호차가 없으면 빈 목록을 반환한다")
            void 호차없음_빈목록반환() {
                Schedule schedule = schedule();
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID)).willReturn(schedule);
                given(coachRepository.findCoachSeatSummaries(eq(schedule), eq(SeatClass.FIRST), any(LocalDateTime.class)))
                        .willReturn(List.of());

                CoachListResponse result = seatService.getCoaches(SCHEDULE_ID, SeatClass.FIRST);

                assertThat(result.coaches()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("스케줄이 없으면 SCHEDULE_NOT_FOUND를 던진다")
            void 스케줄없음_예외() {
                given(scheduleRepository.getByIdOrThrow(SCHEDULE_ID))
                        .willThrow(new GeneralException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

                assertThatThrownBy(() -> seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
                verify(coachRepository, never()).findCoachSeatSummaries(any(), any(), any());
            }
        }
    }
}
