package org.sopt.korail.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ENUMS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.sopt.korail.architecture.ArchitectureTestSupport.CLASSES;

@DisplayName("Entity 아키텍처 테스트")
class EntityArchitectureTest {

    @Nested
    @DisplayName("구조")
    class Structure {

        @Test
        @DisplayName("@Entity는 domain 패키지에 있다")
        void 엔티티_위치() {
            classes().that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage("..domain..")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("엔티티에는 public 생성자를 두지 않는다")
        void public생성자_금지() {
            constructors().that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                    .should().notBePublic()
                    .check(CLASSES);
        }

        @Test
        @DisplayName("엔티티에는 protected 기본 생성자가 있다")
        void protected기본생성자() {
            classes().that().areAnnotatedWith(Entity.class)
                    .should(haveProtectedNoArgsConstructor())
                    .check(CLASSES);
        }

        @Test
        @DisplayName("엔티티에는 public setter를 두지 않는다")
        void setter_금지() {
            noMethods().that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                    .and().arePublic()
                    .should().haveNameStartingWith("set")
                    .because("상태 변경은 의미 있는 이름의 엔티티 메서드로 합니다")
                    .check(CLASSES);
        }
    }

    @Nested
    @DisplayName("매핑")
    class Mapping {

        @Test
        @DisplayName("@ManyToOne, @OneToOne은 fetch = LAZY로 지정한다")
        void 지연로딩() {
            fields().that().areAnnotatedWith(ManyToOne.class).or().areAnnotatedWith(OneToOne.class)
                    .should().beAnnotatedWith(lazyFetch())
                    .check(CLASSES);
        }

        @Test
        @DisplayName("엔티티의 enum 필드에는 @Enumerated(EnumType.STRING)을 붙인다")
        void enum필드_STRING() {
            fields().that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                    .and().haveRawType(ENUMS)
                    .should().beAnnotatedWith(enumeratedString())
                    .check(CLASSES);
        }
    }

    private static DescribedPredicate<JavaAnnotation<?>> lazyFetch() {
        return DescribedPredicate.describe("fetch = FetchType.LAZY", annotation -> {
            if (annotation.getRawType().isEquivalentTo(ManyToOne.class)) {
                return annotation.as(ManyToOne.class).fetch() == FetchType.LAZY;
            }
            if (annotation.getRawType().isEquivalentTo(OneToOne.class)) {
                return annotation.as(OneToOne.class).fetch() == FetchType.LAZY;
            }
            return false;
        });
    }

    private static DescribedPredicate<JavaAnnotation<?>> enumeratedString() {
        return DescribedPredicate.describe(
                "@Enumerated(EnumType.STRING)",
                annotation -> annotation.getRawType().isEquivalentTo(Enumerated.class)
                        && annotation.as(Enumerated.class).value() == EnumType.STRING
        );
    }

    private static ArchCondition<JavaClass> haveProtectedNoArgsConstructor() {
        return new ArchCondition<>("have a protected no-args constructor") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean satisfied = javaClass.getConstructors().stream()
                        .anyMatch(it -> it.getRawParameterTypes().isEmpty()
                                && it.getModifiers().contains(JavaModifier.PROTECTED));
                String message = javaClass.getName() + " does not have a protected no-args constructor";
                events.add(new SimpleConditionEvent(javaClass, satisfied, message));
            }
        };
    }
}
