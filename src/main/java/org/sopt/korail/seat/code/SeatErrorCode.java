package org.sopt.korail.seat.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SeatErrorCode implements BaseErrorCode {
    COACH_NOT_FOUND(HttpStatus.NOT_FOUND, "COACH4041", "존재하지 않는 호차입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
