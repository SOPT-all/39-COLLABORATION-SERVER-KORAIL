package org.sopt.korail.reservation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sopt.korail.support.fixture.MemberFixture.MEMBER_ID;
import static org.sopt.korail.support.fixture.MemberFixture.OTHER_MEMBER_ID;
import static org.sopt.korail.support.fixture.ReservationFixture.reservation;
import static org.sopt.korail.support.fixture.ScheduleFixture.schedule;

@DisplayName("Reservation 단위 테스트")
class ReservationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 12, 0);

    @Nested
    @DisplayName("예약 소유 여부(isOwnedBy)")
    class IsOwnedBy {

        @Test
        @DisplayName("예약한 회원의 ID이면 true를 반환한다")
        void 본인예약_true() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW);

            assertThat(reservation.isOwnedBy(MEMBER_ID)).isTrue();
        }

        @Test
        @DisplayName("다른 회원의 ID이면 false를 반환한다")
        void 다른회원예약_false() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW);

            assertThat(reservation.isOwnedBy(OTHER_MEMBER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("결제 기한 만료 여부(isExpired)")
    class IsExpired {

        @Test
        @DisplayName("결제 대기 예약의 결제 기한이 지났으면 만료로 본다")
        void 대기예약_기한지남_만료() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW.minusMinutes(1));

            assertThat(reservation.isExpired(NOW)).isTrue();
        }

        @Test
        @DisplayName("결제 대기 예약의 결제 기한과 현재 시각이 같으면 만료로 본다")
        void 대기예약_기한경계_만료() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW);

            assertThat(reservation.isExpired(NOW)).isTrue();
        }

        @Test
        @DisplayName("결제 대기 예약의 결제 기한이 남았으면 만료로 보지 않는다")
        void 대기예약_기한남음_유효() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW.plusMinutes(1));

            assertThat(reservation.isExpired(NOW)).isFalse();
        }

        @Test
        @DisplayName("결제 완료 예약은 결제 기한이 지나도 만료로 보지 않는다")
        void 결제완료예약_기한지남_유효() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PAID, NOW.minusMinutes(1));

            assertThat(reservation.isExpired(NOW)).isFalse();
        }
    }

    @Nested
    @DisplayName("현재 시각 기준 예약 상태(getStatusAt)")
    class GetStatusAt {

        @Test
        @DisplayName("결제 기한이 지난 결제 대기 예약이면 EXPIRED를 반환한다")
        void 만료된대기예약_EXPIRED() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW);

            assertThat(reservation.getStatusAt(NOW)).isEqualTo(ReservationStatus.EXPIRED);
        }

        @Test
        @DisplayName("결제 기한이 남은 결제 대기 예약이면 PENDING을 반환한다")
        void 유효한대기예약_PENDING() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PENDING, NOW.plusMinutes(1));

            assertThat(reservation.getStatusAt(NOW)).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("결제 대기가 아닌 예약이면 결제 기한과 상관없이 저장된 상태를 반환한다")
        void 결제완료예약_저장된상태() {
            Reservation reservation = reservation(schedule(), ReservationStatus.PAID, NOW.minusMinutes(1));

            assertThat(reservation.getStatusAt(NOW)).isEqualTo(ReservationStatus.PAID);
        }
    }
}
