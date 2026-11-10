package org.sopt.korail.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.sopt.korail.global.code.BaseErrorCode;
import org.sopt.korail.global.code.BaseSuccessCode;
import org.sopt.korail.global.response.ApiResponse;
import org.sopt.korail.global.swagger.ErrorCodeMarker;
import org.sopt.korail.global.swagger.SuccessCodeMarker;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.Annotation;

@Configuration
public class SwaggerConfig {
    // 모든 API에 Member-Id 헤더 인증 적용
    // 필요 없으면 @SecurityRequirements로 제외
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("코레일 API")
                .description("코레일 API 명세서")
                .version("1.0.0");

        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes("memberId", new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Member-Id")))
                .addSecurityItem(new SecurityRequirement().addList("memberId"));
    }

    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            for (Annotation annotation : handlerMethod.getMethod().getAnnotations()) {
                Class<? extends Annotation> type = annotation.annotationType();

                if (type.isAnnotationPresent(SuccessCodeMarker.class)) {
                    applySuccessCode(operation, (BaseSuccessCode) readValue(annotation));
                } else if (type.isAnnotationPresent(ErrorCodeMarker.class)) {
                    for (BaseErrorCode errorCode : (BaseErrorCode[]) readValue(annotation)) {
                        addErrorExample(operation, errorCode);
                    }
                }
            }

            return operation;
        };
    }

    // 어노테이션의 value() 꺼내기
    private Object readValue(Annotation annotation) {
        try {
            return annotation.annotationType().getMethod("value").invoke(annotation);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(annotation.annotationType().getSimpleName() + "에 value()가 없습니다.", e);
        }
    }

    // 에러 예시 추가
    private void addErrorExample(Operation operation, BaseErrorCode errorCode) {
        String status = String.valueOf(errorCode.getHttpStatus().value());

        var response = operation.getResponses().computeIfAbsent(status, key -> new io.swagger.v3.oas.models.responses.ApiResponse().description(errorCode.getMessage()));
        if (response.getContent() == null) {
            response.setContent(new Content());
        }
        MediaType mediaType = response.getContent().computeIfAbsent("application/json", key -> new MediaType());

        mediaType.addExamples(errorCode.getCode(), new Example().value(ApiResponse.fail(errorCode)));
    }

    // 성공 상태 코드로 변경
    private void applySuccessCode(Operation operation, BaseSuccessCode successCode) {
        ApiResponses responses = operation.getResponses();
        var response = responses.remove("200");
        if (response == null) {
            return;
        }

        String statusCode = String.valueOf(successCode.getHttpStatus().value());
        responses.addApiResponse(statusCode, response.description(successCode.getMessage()));
    }
}
