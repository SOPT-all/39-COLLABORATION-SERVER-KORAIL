package org.sopt.korail.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static org.sopt.korail.architecture.ArchitectureTestSupport.CLASSES;

@DisplayName("DTO 아키텍처 테스트")
class DtoArchitectureTest {

    @Test
    @DisplayName("dto 패키지의 클래스는 record로 작성한다")
    void DTO_record() {
        classes().that().resideInAPackage("..dto..").and().areTopLevelClasses()
                .should().beAssignableTo(Record.class)
                .check(CLASSES);
    }

    @Test
    @DisplayName("DTO 필드에 엔티티를 담지 않는다")
    void DTO_엔티티필드_금지() {
        fields().that().areDeclaredInClassesThat().resideInAPackage("..dto..")
                .and().doNotHaveModifier(JavaModifier.STATIC)
                .should(notInvolveEntityTypes())
                .check(CLASSES);
    }

    @Test
    @DisplayName("요청/응답 DTO의 필드에는 @Schema를 붙인다")
    void 요청응답DTO_Schema() {
        fields().that().areDeclaredInClassesThat().resideInAnyPackage("..dto.request..", "..dto.response..")
                .and().areDeclaredInClassesThat().areTopLevelClasses()
                .and().doNotHaveModifier(JavaModifier.STATIC)
                .should().beAnnotatedWith(Schema.class)
                .check(CLASSES);
    }

    // List<Seat>처럼 제네릭 인자로 감싼 엔티티도 잡아낸다
    private static ArchCondition<JavaField> notInvolveEntityTypes() {
        return new ArchCondition<>("not have an @Entity type (including type arguments)") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                for (JavaClass type : field.getAllInvolvedRawTypes()) {
                    if (type.isAnnotatedWith(Entity.class)) {
                        String message = field.getFullName() + " involves entity " + type.getName();
                        events.add(SimpleConditionEvent.violated(field, message));
                    }
                }
            }
        };
    }
}
