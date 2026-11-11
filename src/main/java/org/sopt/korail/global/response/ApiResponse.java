package org.sopt.korail.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sopt.korail.global.code.BaseCode;
import org.sopt.korail.global.code.BaseErrorCode;
import org.sopt.korail.global.code.BaseSuccessCode;

public record ApiResponse<T>(
        @JsonProperty("isSuccess") boolean isSuccess,
        String code,
        String message,
        T result
) {
    // 성공
    public static <T> ApiResponse<T> success(BaseSuccessCode successCode, T result) {
        return new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), result);
    }

    // 실패
    public static <T> ApiResponse<T> fail(BaseErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    // 스프링 예외의 상태 코드를 가져다 쓰는데 필요
    public static <T> ApiResponse<T> fail(String errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null);
    }
}
