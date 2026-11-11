package org.sopt.korail.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sopt.korail.global.code.BaseCode;
import org.sopt.korail.global.code.BaseErrorCode;
import org.sopt.korail.global.code.BaseSuccessCode;
import org.sopt.korail.global.code.CommonSuccessCode;
import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(
        @JsonProperty("isSuccess") boolean isSuccess,
        String code,
        String message,
        T data
) {
    // 성공
    public static <T> ResponseEntity<ApiResponse<T>> success(BaseSuccessCode successCode, T data) {
        return ResponseEntity
                .status(successCode.getHttpStatus())
                .body(of(successCode, data));
    }

    // 실패
    public static <T> ApiResponse<T> fail(BaseErrorCode errorCode) {
        return of(errorCode, null);
    }

    // 스프링 예외의 상태 코드를 가져다 쓰는데 필요
    public static <T> ApiResponse<T> fail(String errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null);
    }

    private static <T> ApiResponse<T> of(BaseCode code, T data) {
        boolean isSuccess = !(code instanceof BaseErrorCode);
        return new ApiResponse<>(isSuccess, code.getCode(), code.getMessage(), data);
    }
}
