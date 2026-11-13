package org.sopt.korail.schedule.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.korail.global.code.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScheduleSuccessCode implements BaseSuccessCode {
    SCHEDULE_LIST_OK(HttpStatus.OK, "SCHEDULE2001", "열차 목록 조회에 성공했습니다."),
    SCHEDULE_DETAIL_OK(HttpStatus.OK, "SCHEDULE2002", "열차 상세 및 운임 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
