package org.sopt.korail.seat.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.korail.global.enums.SeatClass;
import org.sopt.korail.schedule.domain.Train;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_coach_train_coach_number", columnNames = {"train_id", "coach_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(nullable = false)
    private int coachNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeatClass seatClass;

    @Builder
    private Coach(
            Train train,
            int coachNumber,
            SeatClass seatClass
    ) {
        this.train = train;
        this.coachNumber = coachNumber;
        this.seatClass = seatClass;
    }
}
