package org.sopt.korail.reservation.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationSuccessCode implements BaseSuccessCode {
    CREATE_RESERVATION(HttpStatus.CREATED, "RESERVATION2011", "승차권 예약에 성공했습니다."),
    GET_RESERVATION(HttpStatus.OK, "RESERVATION2001", "승차권 예약 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
