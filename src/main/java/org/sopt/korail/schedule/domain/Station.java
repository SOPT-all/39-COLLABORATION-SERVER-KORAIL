package org.sopt.korail.schedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import org.hibernate.annotations.ColumnDefault;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @ColumnDefault("false")
    @Column(name = "is_major", nullable = false)
    private boolean major = false;

    @Builder
    private Station(
            String name,
            boolean major
    ) {
        this.name = name;
        this.major = major;
    }
}
