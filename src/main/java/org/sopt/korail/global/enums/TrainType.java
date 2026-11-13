package org.sopt.korail.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainType {
    KTX("KTX"),
    ITX("ITX-새마을"),
    MUGUNGHWA("무궁화");

    private final String displayName;
}
