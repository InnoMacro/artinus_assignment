package org.artinus.backend.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

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

    @Test
    fun `domain은 framework와 serialization 기술에 의존하지 않는다`() {
        noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(*DOMAIN_FORBIDDEN_PACKAGES)
            .check(productionClasses)
    }

    @Test
    fun `application은 web persistence provider 기술에 의존하지 않는다`() {
        noClasses()
            .that().resideInAnyPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(*APPLICATION_FORBIDDEN_PACKAGES)
            .check(productionClasses)
    }

    @Test
    fun `inbound와 outbound adapter는 서로 직접 의존하지 않는다`() {
        noClasses()
            .that().resideInAnyPackage("..adapter.inbound..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter.outbound..")
            .check(productionClasses)

        noClasses()
            .that().resideInAnyPackage("..adapter.outbound..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter.inbound..")
            .check(productionClasses)
    }

    @Test
    fun `outbound 기술 adapter는 서로 직접 의존하지 않는다`() {
        slices()
            .matching("..adapter.outbound.(*)..")
            .should().notDependOnEachOther()
            .check(productionClasses)
    }

    @Test
    fun `최상위 feature 사이에는 순환 의존이 없다`() {
        slices()
            .matching("org.artinus.backend.(*)..")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }

    @Test
    fun `framework 진입 타입은 정해진 adapter와 config에 위치한다`() {
        classes()
            .that().areAnnotatedWith(Entity::class.java)
            .or().areAnnotatedWith(Converter::class.java)
            .or().areAssignableTo(JpaRepository::class.java)
            .should().resideInAnyPackage("..adapter.outbound.persistence..")
            .check(productionClasses)

        classes()
            .that().areAnnotatedWith(RestController::class.java)
            .should().resideInAnyPackage("..adapter.inbound.web..")
            .check(productionClasses)

        classes()
            .that().areAnnotatedWith(RestControllerAdvice::class.java)
            .should().resideInAnyPackage("..adapter.inbound.web..", "..common.error..")
            .check(productionClasses)

        classes()
            .that().areAnnotatedWith(ConfigurationProperties::class.java)
            .should().resideInAnyPackage("..config..")
            .check(productionClasses)

        classes()
            .that().areAnnotatedWith(Configuration::class.java)
            .should().resideInAnyPackage("org.artinus.backend.config..", "..adapter..config..")
            .check(productionClasses)
    }

    @Test
    fun `adapter 내부 계약 타입은 소유 adapter 밖으로 노출되지 않는다`() {
        noClasses()
            .that().resideOutsideOfPackages("..adapter.inbound.web..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter.inbound.web.request..", "..adapter.inbound.web.response..")
            .check(productionClasses)

        noClasses()
            .that().resideOutsideOfPackages("..adapter.outbound.csrng..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..adapter.outbound.csrng.config..",
                "..adapter.outbound.csrng.exception..",
                "..adapter.outbound.csrng.response..",
            )
            .check(productionClasses)

        noClasses()
            .that().resideOutsideOfPackages("..adapter.outbound.ai..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter.outbound.ai.config..", "..adapter.outbound.ai.prompt..")
            .check(productionClasses)
    }

    companion object {
        private val DOMAIN_FORBIDDEN_PACKAGES =
            arrayOf(
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..",
                "com.querydsl..",
                "org.springframework.ai..",
                "com.openai..",
                "io.github.resilience4j..",
                "tools.jackson..",
                "com.fasterxml.jackson..",
            )

        private val APPLICATION_FORBIDDEN_PACKAGES =
            arrayOf(
                "org.springframework.web..",
                "org.springframework.http..",
                "org.springframework.data..",
                "jakarta.persistence..",
                "jakarta.validation..",
                "org.hibernate..",
                "com.querydsl..",
                "org.springframework.ai..",
                "com.openai..",
                "io.github.resilience4j..",
                "tools.jackson..",
                "com.fasterxml.jackson..",
            )
    }
}
