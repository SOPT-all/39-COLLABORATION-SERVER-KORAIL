package org.sopt.korail.reservation.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements BaseErrorCode {
    SEAT_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "RESERVATION4001", "선택한 좌석 수가 인원 수와 다릅니다."),
    SEAT_NOT_IN_SCHEDULE(HttpStatus.BAD_REQUEST, "RESERVATION4002", "선택한 일정의 열차 좌석이 아닙니다."),
    SEAT_CLASS_MISMATCH(HttpStatus.BAD_REQUEST, "RESERVATION4003", "선택한 좌석 등급과 다른 좌석이 포함되어 있습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION4041", "존재하지 않는 예약입니다."),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "RESERVATION4091", "이미 예약된 좌석이 포함되어 있습니다."),
    INSUFFICIENT_SEATS(HttpStatus.CONFLICT, "RESERVATION4092", "잔여 좌석이 부족합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
