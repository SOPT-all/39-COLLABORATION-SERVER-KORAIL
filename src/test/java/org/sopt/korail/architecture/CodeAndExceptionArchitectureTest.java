package org.sopt.korail.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.code.BaseErrorCode;
import org.sopt.korail.global.code.BaseSuccessCode;
import org.sopt.korail.global.exception.GeneralException;
import org.sopt.korail.global.swagger.ErrorCodeMarker;
import org.sopt.korail.global.swagger.SuccessCodeMarker;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static org.sopt.korail.architecture.ArchitectureTestSupport.CLASSES;

@DisplayName("응답 코드, 예외, 공통 규칙 아키텍처 테스트")
class CodeAndExceptionArchitectureTest {

    @Nested
    @DisplayName("응답 코드")
    class ResponseCode {

        @Test
        @DisplayName("BaseErrorCode 구현체는 code 패키지의 enum이고 이름이 ErrorCode로 끝난다")
        void 에러코드_enum() {
            classes().that().implement(BaseErrorCode.class)
                    .should().beEnums()
                    .andShould().resideInAPackage("..code..")
                    .andShould().haveSimpleNameEndingWith("ErrorCode")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("BaseSuccessCode 구현체는 code 패키지의 enum이고 이름이 SuccessCode로 끝난다")
        void 성공코드_enum() {
            classes().that().implement(BaseSuccessCode.class)
                    .should().beEnums()
                    .andShould().resideInAPackage("..code..")
                    .andShould().haveSimpleNameEndingWith("SuccessCode")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("@SuccessCodeMarker가 붙은 어노테이션은 이름이 Api{Domain}Success 형태다")
        void 성공코드_어노테이션_이름() {
            classes().that().areAnnotations().and().areAnnotatedWith(SuccessCodeMarker.class)
                    .should().haveSimpleNameStartingWith("Api")
                    .andShould().haveSimpleNameEndingWith("Success")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("@ErrorCodeMarker가 붙은 어노테이션은 이름이 Api{Domain}Errors 형태다")
        void 에러코드_어노테이션_이름() {
            classes().that().areAnnotations().and().areAnnotatedWith(ErrorCodeMarker.class)
                    .should().haveSimpleNameStartingWith("Api")
                    .andShould().haveSimpleNameEndingWith("Errors")
                    .check(CLASSES);
        }
    }

    @Nested
    @DisplayName("예외")
    class Exception {

        @Test
        @DisplayName("GeneralException 외의 예외 클래스를 만들지 않는다")
        void 커스텀예외_금지() {
            classes().that().areAssignableTo(Throwable.class)
                    .should().haveFullyQualifiedName(GeneralException.class.getName())
                    .check(CLASSES);
        }

        @Test
        @DisplayName("도메인 코드는 GeneralException 외의 예외를 생성하지 않는다")
        void 도메인_표준예외_금지() {
            noClasses().that().resideOutsideOfPackage("..global..")
                    .should().callConstructorWhere(nonGeneralExceptionConstructor())
                    .because("비즈니스 예외는 GeneralException({Domain}ErrorCode)로만 던집니다")
                    .check(CLASSES);
        }
    }

    @Nested
    @DisplayName("공통")
    class Common {

        @Test
        @DisplayName("필드 주입(@Autowired 등)을 사용하지 않는다")
        void 필드주입_금지() {
            NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(CLASSES);
        }
    }

    private static DescribedPredicate<JavaConstructorCall> nonGeneralExceptionConstructor() {
        return DescribedPredicate.describe(
                "a Throwable other than GeneralException is constructed",
                call -> call.getTargetOwner().isAssignableTo(Throwable.class)
                        && !call.getTargetOwner().isEquivalentTo(GeneralException.class)
        );
    }
}
