package org.sopt.korail.schedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.korail.global.enums.TrainType;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String trainNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainType trainType;

    @Builder
    private Train(
            String trainNumber,
            TrainType trainType
    ) {
        this.trainNumber = trainNumber;
        this.trainType = trainType;
    }
}
