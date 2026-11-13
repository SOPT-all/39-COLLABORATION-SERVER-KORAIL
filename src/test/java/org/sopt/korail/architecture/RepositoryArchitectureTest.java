package org.sopt.korail.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.sopt.korail.architecture.ArchitectureTestSupport.CLASSES;

@DisplayName("Repository 아키텍처 테스트")
class RepositoryArchitectureTest {

    @Test
    @DisplayName("JpaRepository를 상속한 인터페이스는 repository 패키지에 있고 이름이 Repository로 끝난다")
    void 리포지토리_위치와_이름() {
        classes().that().areAssignableTo(JpaRepository.class).and().areInterfaces()
                .and().doNotHaveFullyQualifiedName(JpaRepository.class.getName())
                .should().resideInAPackage("..repository..")
                .andShould().haveSimpleNameEndingWith("Repository")
                .check(CLASSES);
    }

    @Test
    @DisplayName("이름이 Repository로 끝나는 인터페이스는 JpaRepository를 상속한다")
    void 리포지토리_JpaRepository_상속() {
        classes().that().resideInAPackage("..repository..")
                .and().areInterfaces()
                .and().haveSimpleNameEndingWith("Repository")
                .should().beAssignableTo(JpaRepository.class)
                .check(CLASSES);
    }
}
