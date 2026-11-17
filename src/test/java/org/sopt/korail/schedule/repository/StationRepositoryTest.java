package org.sopt.korail.schedule.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.config.JpaConfig;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.schedule.code.ScheduleErrorCode;
import org.sopt.korail.schedule.domain.Station;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
@DisplayName("StationRepository 테스트")
class StationRepositoryTest {

    private static final String NOT_EXIST_NAME = "없는역";

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StationRepository stationRepository;

    @Nested
    @DisplayName("역 존재 검증(validateExists)")
    class ValidateExists {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("역이 있으면 예외를 던지지 않는다")
            void 역있음_통과() {
                persistStation("서울");

                assertThat(stationRepository.existsByName("서울")).isTrue();
                assertThatCode(() -> stationRepository.validateExists("서울")).doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("역이 없으면 STATION_NOT_FOUND 예외가 발생한다")
            void 역없음_STATION_NOT_FOUND() {
                assertThatThrownBy(() -> stationRepository.validateExists(NOT_EXIST_NAME))
                        .isInstanceOfSatisfying(GeneralException.class,
                                e -> assertThat(e.getErrorCode()).isEqualTo(ScheduleErrorCode.STATION_NOT_FOUND));
            }
        }
    }

    private Station persistStation(String name) {
        return em.persistAndFlush(Station.builder()
                .name(name)
                .major(true)
                .build());
    }
}
