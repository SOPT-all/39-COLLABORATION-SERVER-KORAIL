package org.sopt.korail.seat.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SeatSuccessCode implements BaseSuccessCode {
    COACH_LIST_OK(HttpStatus.OK, "SEAT2001", "호차 목록 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
