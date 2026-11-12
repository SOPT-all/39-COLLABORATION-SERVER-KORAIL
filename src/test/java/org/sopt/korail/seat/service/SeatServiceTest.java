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
import org.sopt.korail.seat.dto.CoachSeatSummary;
import org.sopt.korail.seat.dto.response.CoachListResponse;
import org.sopt.korail.seat.dto.response.CoachResponse;
import org.sopt.korail.seat.repository.CoachRepository;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatService 단위 테스트")
class SeatServiceTest {
    @InjectMocks private SeatService seatService;

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CoachRepository coachRepository;

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
                given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(coachRepository.findCoachSeatSummaries(eq(schedule), eq(SeatClass.STANDARD), any(LocalDateTime.class)))
                        .willReturn(List.of(new CoachSeatSummary(10L, 1, 48, 31)));

                CoachListResponse result = seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD);

                assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
                assertThat(result.seatClass()).isEqualTo(SeatClass.STANDARD);
                assertThat(result.coaches()).containsExactly(CoachResponse.builder()
                        .coachId(10L)
                        .coachNumber(1)
                        .remainingSeats(17)
                        .totalSeats(48)
                        .reservable(true)
                        .build());
            }

            @Test
            @DisplayName("잔여 좌석이 없으면 선택 불가로 반환한다")
            void 잔여석없음_선택불가() {
                Schedule schedule = schedule();
                given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
                given(coachRepository.findCoachSeatSummaries(eq(schedule), eq(SeatClass.STANDARD), any(LocalDateTime.class)))
                        .willReturn(List.of(new CoachSeatSummary(10L, 3, 48, 48)));

                CoachListResponse result = seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD);

                assertThat(result.coaches()).singleElement()
                        .satisfies(coach -> {
                            assertThat(coach.remainingSeats()).isZero();
                            assertThat(coach.reservable()).isFalse();
                        });
            }

            @Test
            @DisplayName("해당 좌석 등급의 호차가 없으면 빈 목록을 반환한다")
            void 호차없음_빈목록반환() {
                Schedule schedule = schedule();
                given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
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
                given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> seatService.getCoaches(SCHEDULE_ID, SeatClass.STANDARD))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getErrorCode())
                                .isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND));
                verify(coachRepository, never()).findCoachSeatSummaries(any(), any(), any());
            }
        }
    }
}
