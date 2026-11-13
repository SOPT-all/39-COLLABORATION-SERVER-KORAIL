package org.sopt.korail.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sopt.korail.global.swagger.SuccessCodeMarker;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.sopt.korail.architecture.ArchitectureTestSupport.CLASSES;

@DisplayName("Controller 아키텍처 테스트")
class ControllerArchitectureTest {

    @Nested
    @DisplayName("구조")
    class Structure {

        @Test
        @DisplayName("@RestController는 controller 패키지에 있고 이름이 Controller로 끝난다")
        void 컨트롤러_위치와_이름() {
            classes().that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..controller..")
                    .andShould().haveSimpleNameEndingWith("Controller")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("@RestController는 {Domain}Api 스웨거 인터페이스를 구현한다")
        void 컨트롤러_Api인터페이스_구현() {
            classes().that().areAnnotatedWith(RestController.class)
                    .should(implementApiInterface())
                    .check(CLASSES);
        }

        @Test
        @DisplayName("controller 패키지의 인터페이스는 이름이 Api로 끝나고 @Tag가 붙어 있다")
        void Api인터페이스_이름과_Tag() {
            classes().that().resideInAPackage("..controller..").and().areInterfaces()
                    .should().haveSimpleNameEndingWith("Api")
                    .andShould().beAnnotatedWith(Tag.class)
                    .check(CLASSES);
        }

        @Test
        @DisplayName("@RequestMapping은 구현 클래스가 아닌 Api 인터페이스에 둔다")
        void 컨트롤러_RequestMapping_금지() {
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().beAnnotatedWith(RequestMapping.class)
                    .check(CLASSES);
        }
    }

    @Nested
    @DisplayName("API 메서드")
    class ApiMethod {

        @Test
        @DisplayName("컨트롤러의 public 메서드는 dto 패키지의 타입만 반환한다")
        void DTO만_반환() {
            methods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .and().arePublic()
                    .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
                    .should().haveRawReturnType(resideInAPackage("..dto.."))
                    .because("ApiResponse, ResponseEntity, String, void, 엔티티를 반환하지 않고 DTO만 반환합니다")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("컨트롤러의 public 메서드에는 @Api{Domain}Success가 붙어 있다")
        void 성공코드_어노테이션() {
            methods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .and().arePublic()
                    .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
                    .should().beMetaAnnotatedWith(SuccessCodeMarker.class)
                    .check(CLASSES);
        }
    }

    private static ArchCondition<JavaClass> implementApiInterface() {
        return new ArchCondition<>("implement an interface named *Api") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean satisfied = javaClass.getRawInterfaces().stream()
                        .anyMatch(it -> it.getSimpleName().endsWith("Api"));
                String message = javaClass.getName() + " does not implement an interface named *Api";
                events.add(new SimpleConditionEvent(javaClass, satisfied, message));
            }
        };
    }
}
