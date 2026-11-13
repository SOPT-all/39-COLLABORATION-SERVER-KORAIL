package org.sopt.korail.schedule.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements BaseErrorCode {
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE4041", "존재하지 않는 일정입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
