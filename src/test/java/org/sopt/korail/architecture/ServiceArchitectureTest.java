package org.sopt.korail.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.sopt.korail.architecture.ArchitectureTestSupport.CLASSES;

@DisplayName("Service 아키텍처 테스트")
class ServiceArchitectureTest {

    @Test
    @DisplayName("service 패키지의 클래스는 인터페이스 없이 @Service 구현 클래스로 작성한다")
    void 서비스_구현클래스() {
        classes().that().resideInAPackage("..service..").and().areTopLevelClasses()
                .should().notBeInterfaces()
                .andShould().beAnnotatedWith(Service.class)
                .andShould().haveSimpleNameEndingWith("Service")
                .check(CLASSES);
    }

    @Test
    @DisplayName("@Service는 service 패키지에 있다")
    void 서비스_위치() {
        classes().that().areAnnotatedWith(Service.class)
                .should().resideInAPackage("..service..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("@Service 클래스에는 @Transactional(readOnly = true)가 붙어 있다")
    void 서비스_읽기전용_트랜잭션() {
        classes().that().areAnnotatedWith(Service.class)
                .should().beAnnotatedWith(readOnlyTransactional())
                .check(CLASSES);
    }

    private static DescribedPredicate<JavaAnnotation<?>> readOnlyTransactional() {
        return DescribedPredicate.describe(
                "@Transactional(readOnly = true)",
                annotation -> annotation.getRawType().isEquivalentTo(Transactional.class)
                        && annotation.as(Transactional.class).readOnly()
        );
    }
}
