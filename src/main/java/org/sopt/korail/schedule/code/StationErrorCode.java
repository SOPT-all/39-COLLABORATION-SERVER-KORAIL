package org.sopt.korail.schedule.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StationErrorCode implements BaseErrorCode {
    STATION_NOT_FOUND(HttpStatus.NOT_FOUND, "STATION4041", "존재하지 않는 역입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
