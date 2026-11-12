package org.sopt.korail.schedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.korail.global.enums.PassengerType;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.global.enums.TrainType;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Fare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_station_id", nullable = false)
    private Station departureStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_station_id", nullable = false)
    private Station arrivalStation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainType trainType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeatClass seatClass;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PassengerType passengerType;

    @Builder
    private Fare(
            Station departureStation,
            Station arrivalStation,
            TrainType trainType,
            SeatClass seatClass,
            int price,
            PassengerType passengerType
    ) {
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.trainType = trainType;
        this.seatClass = seatClass;
        this.price = price;
        this.passengerType = passengerType;
    }
}
