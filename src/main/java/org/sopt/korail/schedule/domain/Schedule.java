package org.sopt.korail.schedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_station_id", nullable = false)
    private Station departureStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_station_id", nullable = false)
    private Station arrivalStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(nullable = false)
    private LocalDateTime departureAt;

    @Column(nullable = false)
    private LocalDateTime arrivalAt;

    @Builder
    private Schedule(
            Station departureStation,
            Station arrivalStation,
            Train train,
            LocalDateTime departureAt,
            LocalDateTime arrivalAt
    ) {
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.train = train;
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
    }
}
