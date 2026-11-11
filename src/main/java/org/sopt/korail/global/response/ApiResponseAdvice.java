package org.sopt.korail.global.response;

import org.sopt.korail.global.code.BaseSuccessCode;
import org.sopt.korail.global.swagger.SuccessCodeMarker;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.lang.annotation.Annotation;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {
    // @SuccessCodeMarker가 붙은 어노테이션이 있는 메서드만
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !StringHttpMessageConverter.class.isAssignableFrom(converterType) && findSuccessCode(returnType) != null;
    }

    // ApiResponse로 감싸고 상태 코드 설정
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        BaseSuccessCode successCode = findSuccessCode(returnType);
        response.setStatusCode(successCode.getHttpStatus());
        return ApiResponse.success(successCode, body);
    }

    // 어노테이션 중 @SuccessCodeMarker가 붙은 것의 value()를 꺼냄
    private BaseSuccessCode findSuccessCode(MethodParameter returnType) {
        for (Annotation annotation : returnType.getMethodAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(SuccessCodeMarker.class)) {
                try {
                    return (BaseSuccessCode) annotation.annotationType()
                            .getMethod("value").invoke(annotation);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return null;
    }
}

