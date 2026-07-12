package org.artinus.backend.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class LayerDependencyTest {
    private val productionClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.artinus.backend")

    @Test
    fun `domain은 application과 adapter에 의존하지 않는다`() {
        noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..adapter..")
            .check(productionClasses)
    }

    @Test
    fun `application은 adapter에 의존하지 않는다`() {
        noClasses()
            .that().resideInAnyPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter..")
            .check(productionClasses)
    }
}
