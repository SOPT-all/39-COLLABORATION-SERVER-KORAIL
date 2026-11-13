package org.sopt.korail.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainType {
    KTX("KTX"),
    ITX("ITX"),
    MUGUNGHWA("무궁화호");

    private final String displayName;
}
