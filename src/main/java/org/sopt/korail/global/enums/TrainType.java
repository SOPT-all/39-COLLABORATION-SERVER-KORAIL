package org.sopt.korail.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainType {
    KTX("KTX"),
    ITX_SAEMAUL("ITX-새마을"),
    ITX_CHEONGCHUN("ITX-청춘"),
    MUGUNGHWA("무궁화호");

    private final String displayName;
}
