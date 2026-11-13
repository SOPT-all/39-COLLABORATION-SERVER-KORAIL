package org.sopt.korail.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

// 아키텍처 테스트가 공유하는 검사 대상 클래스
final class ArchitectureTestSupport {
    static final String BASE_PACKAGE = "org.sopt.korail";

    static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

    private ArchitectureTestSupport() {
    }
}
