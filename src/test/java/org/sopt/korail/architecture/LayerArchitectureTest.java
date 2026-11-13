package org.sopt.korail.architecture;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static org.sopt.korail.architecture.ArchitectureTestSupport.CLASSES;

@DisplayName("계층 의존성 아키텍처 테스트")
class LayerArchitectureTest {

    @Test
    @DisplayName("Controller → Service → Repository 방향으로만 의존한다")
    void 계층_의존방향() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Repository").definedBy("..repository..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                .check(CLASSES);
    }

    @Test
    @DisplayName("컨트롤러는 엔티티에 의존하지 않는다")
    void 컨트롤러_엔티티의존_금지() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                .check(CLASSES);
    }

    @Test
    @DisplayName("도메인(엔티티) 패키지는 controller, service, repository, dto에 의존하지 않는다")
    void 도메인_상위계층의존_금지() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..controller..", "..service..", "..repository..", "..dto..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("DTO는 controller, service, repository에 의존하지 않는다")
    void DTO_계층의존_금지() {
        noClasses().that().resideInAPackage("..dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..controller..", "..service..", "..repository..")
                .check(CLASSES);
    }
}
