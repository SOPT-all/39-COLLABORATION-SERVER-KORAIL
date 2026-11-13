package org.sopt.korail.schedule.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.config.JpaConfig;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;
import org.sopt.korail.schedule.domain.Fare;
import org.sopt.korail.schedule.domain.Station;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
@DisplayName("FareRepository 테스트")
class FareRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private FareRepository fareRepository;

    private Station seoul;
    private Station busan;
    private Station daejeon;

    @BeforeEach
    void setUp() {
        seoul = em.persist(Station.builder().name("서울").major(true).build());
        busan = em.persist(Station.builder().name("부산").major(true).build());
        daejeon = em.persist(Station.builder().name("대전").major(true).build());
    }

    @Nested
    @DisplayName("운임 단건 조회(findFare)")
    class FindFare {

        @Test
        @DisplayName("구간, 열차 종류, 좌석 등급, 승객 유형이 모두 일치하는 운임을 반환한다")
        void 조건일치_운임반환() {
            persistFare(seoul, busan, TrainType.KTX, SeatClass.STANDARD, PassengerType.ADULT, 59_800);
            persistFare(seoul, busan, TrainType.KTX, SeatClass.FIRST, PassengerType.ADULT, 83_700);
            persistFare(seoul, busan, TrainType.KTX, SeatClass.STANDARD, PassengerType.CHILD, 29_900);
            persistFare(seoul, busan, TrainType.ITX, SeatClass.STANDARD, PassengerType.ADULT, 42_600);
            persistFare(seoul, daejeon, TrainType.KTX, SeatClass.STANDARD, PassengerType.ADULT, 23_700);
            persistFare(busan, seoul, TrainType.KTX, SeatClass.STANDARD, PassengerType.ADULT, 59_000);
            em.flush();
            em.clear();

            Optional<Fare> result = fareRepository.findFare(
                    seoul.getId(), busan.getId(), TrainType.KTX, SeatClass.STANDARD, PassengerType.ADULT);

            assertThat(result).get()
                    .extracting(Fare::getPrice)
                    .isEqualTo(59_800);
        }

        @Test
        @DisplayName("일치하는 운임이 없으면 빈 값을 반환한다")
        void 조건불일치_빈값() {
            persistFare(seoul, busan, TrainType.KTX, SeatClass.STANDARD, PassengerType.ADULT, 59_800);
            em.flush();
            em.clear();

            Optional<Fare> result = fareRepository.findFare(
                    seoul.getId(), busan.getId(), TrainType.KTX, SeatClass.FIRST, PassengerType.ADULT);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("구간 운임 목록 조회(findSectionFares)")
    class FindSectionFares {

        @Test
        @DisplayName("출발역·도착역 이름과 승객 유형이 일치하는 모든 열차 종류·좌석 등급 운임을 반환한다")
        void 구간승객유형일치_운임목록반환() {
            persistFare(seoul, busan, TrainType.KTX, SeatClass.STANDARD, PassengerType.ADULT, 59_800);
            persistFare(seoul, busan, TrainType.KTX, SeatClass.FIRST, PassengerType.ADULT, 83_700);
            persistFare(seoul, busan, TrainType.ITX, SeatClass.STANDARD, PassengerType.ADULT, 42_600);
            persistFare(seoul, busan, TrainType.KTX, SeatClass.STANDARD, PassengerType.CHILD, 29_900);
            persistFare(seoul, daejeon, TrainType.KTX, SeatClass.STANDARD, PassengerType.ADULT, 23_700);
            persistFare(busan, seoul, TrainType.KTX, SeatClass.STANDARD, PassengerType.ADULT, 59_000);
            em.flush();
            em.clear();

            List<Fare> result = fareRepository.findSectionFares("서울", "부산", PassengerType.ADULT);

            assertThat(result)
                    .extracting(Fare::getPrice)
                    .containsExactlyInAnyOrder(59_800, 83_700, 42_600);
        }
    }

    private void persistFare(
            Station departure,
            Station arrival,
            TrainType trainType,
            SeatClass seatClass,
            PassengerType passengerType,
            int price
    ) {
        em.persist(Fare.builder()
                .departureStation(departure)
                .arrivalStation(arrival)
                .trainType(trainType)
                .seatClass(seatClass)
                .passengerType(passengerType)
                .price(price)
                .build());
    }
}
