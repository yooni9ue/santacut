package com.santacut

import com.santacut.application.global.Facade
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import jakarta.persistence.Entity
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 컨벤션 1~3장의 패키지 구성, 계층 의존 방향, facade·service 간 주입 금지, 스테레오타입 위치를 검사한다.
 */
class LayerDependencyTest {
    private val classes: JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(ROOT)

    @Test
    fun `최상위 패키지에는 네 계층 패키지와 애플리케이션 클래스만 있다`() {
        classes().that().resideOutsideOfPackage(ROOT)
            .should().resideInAnyPackage("$PRESENTATION..", "$APPLICATION..", "$DOMAIN..", "$INFRASTRUCTURE..")
            .check(classes)
        classes().that().resideInAPackage(ROOT)
            .should().haveSimpleNameStartingWith(APPLICATION_CLASS_NAME)
            .check(classes)
    }

    @Test
    fun `계층 아래는 도메인 이름과 global로 나누고 그 아래 역할 패키지에만 클래스를 둔다`() {
        classes().that().resideOutsideOfPackage(ROOT)
            .should(resideInDomainRolePackage())
            .check(classes)
    }

    @Test
    fun `presentation은 advice를 빼면 application만 쓰고 domain과 infrastructure를 건너뛰어 쓰지 않는다`() {
        noClasses().that().resideInAPackage("$PRESENTATION..")
            .and().resideOutsideOfPackage("$PRESENTATION.global.advice..")
            .should().dependOnClassesThat().resideInAnyPackage("$DOMAIN..", "$INFRASTRUCTURE..")
            .check(classes)
    }

    @Test
    fun `presentation advice는 domain 중 예외만 참조한다`() {
        noClasses().that().resideInAPackage("$PRESENTATION.global.advice..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("$DOMAIN.*.model..", "$DOMAIN.*.service..", "$INFRASTRUCTURE..")
            .check(classes)
    }

    @Test
    fun `application은 presentation, infrastructure, domain 설정을 참조하지 않는다`() {
        noClasses().that().resideInAPackage("$APPLICATION..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("$PRESENTATION..", "$INFRASTRUCTURE..", "$DOMAIN.*.config..")
            .check(classes)
    }

    @Test
    fun `domain은 presentation과 application을 참조하지 않는다`() {
        noClasses().that().resideInAPackage("$DOMAIN..")
            .should().dependOnClassesThat().resideInAnyPackage("$PRESENTATION..", "$APPLICATION..")
            .check(classes)
    }

    @Test
    fun `domain model은 service와 infrastructure를 참조하지 않는다`() {
        noClasses().that().resideInAPackage("$DOMAIN.*.model..")
            .should().dependOnClassesThat().resideInAnyPackage("$DOMAIN.*.service..", "$INFRASTRUCTURE..")
            .check(classes)
    }

    @Test
    fun `infrastructure는 domain 중 model만 참조한다`() {
        noClasses().that().resideInAPackage("$INFRASTRUCTURE..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "$PRESENTATION..", "$APPLICATION..", "$DOMAIN.*.service..", "$DOMAIN.*.exception..", "$DOMAIN.*.config..",
            )
            .check(classes)
    }

    @Test
    fun `infrastructure의 render와 config는 domain을 참조하지 않는다`() {
        noClasses().that().resideInAnyPackage("$INFRASTRUCTURE.*.render..", "$INFRASTRUCTURE.*.config..")
            .should().dependOnClassesThat().resideInAPackage("$DOMAIN..")
            .check(classes)
    }

    @Test
    fun `infrastructure client는 domain model 중 엔티티를 참조하지 않는다`() {
        noClasses().that().resideInAPackage("$INFRASTRUCTURE.*.client..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity::class.java)
            .check(classes)
    }

    @Test
    fun `facade는 다른 facade를 주입받지 않는다`() {
        classes().that().areAnnotatedWith(Facade::class.java)
            .should(notInjectTypesAnnotatedWith(Facade::class.java))
            .check(classes)
    }

    @Test
    fun `service는 다른 service를 주입받지 않는다`() {
        classes().that().areAnnotatedWith(Service::class.java)
            .should(notInjectTypesAnnotatedWith(Service::class.java))
            .check(classes)
    }

    @Test
    fun `HTTP 진입점은 presentation의 controller와 advice 패키지에 있다`() {
        classes().that().areAnnotatedWith(RestController::class.java)
            .should().resideInAPackage("$PRESENTATION.*.controller").andShould().haveSimpleNameEndingWith("Controller")
            .check(classes)
        classes().that().areAnnotatedWith(RestControllerAdvice::class.java)
            .should().resideInAPackage("$PRESENTATION.global.advice")
            .check(classes)
    }

    @Test
    fun `facade, service, 엔티티는 정해진 패키지에 있고 이름 규칙을 따른다`() {
        classes().that().areAnnotatedWith(Facade::class.java)
            .should().resideInAPackage("$APPLICATION.*.facade").andShould().haveSimpleNameEndingWith("Facade")
            .check(classes)
        classes().that().areAnnotatedWith(Service::class.java)
            .should().resideInAPackage("$DOMAIN.*.service").andShould().haveSimpleNameEndingWith("Service")
            .check(classes)
        classes().that().areAnnotatedWith(Entity::class.java)
            .should().resideInAPackage("$DOMAIN.*.model")
            .check(classes)
    }

    @Test
    fun `범용 Component는 infrastructure의 client와 render에만 있다`() {
        classes().that().areAnnotatedWith(Component::class.java).and().areNotAnnotations()
            .should().resideInAnyPackage("$INFRASTRUCTURE.*.client", "$INFRASTRUCTURE.*.render")
            .check(classes)
    }

    @Test
    fun `설정 클래스는 각 계층의 config와 웹 보안, Swagger 문서 패키지에만 있다`() {
        classes().that().areAnnotatedWith(Configuration::class.java)
            .should().resideInAnyPackage(
                "$PRESENTATION.global.config", "$PRESENTATION.global.security", "$PRESENTATION.global.docs",
                "$DOMAIN.global.config", "$INFRASTRUCTURE.global.config",
            )
            .andShould().haveSimpleNameEndingWith("Configuration")
            .check(classes)
        classes().that().areAnnotatedWith(ConfigurationProperties::class.java)
            .should().resideInAnyPackage("$PRESENTATION.global.config", "$DOMAIN.global.config", "$INFRASTRUCTURE.global.config")
            .andShould().haveSimpleNameEndingWith("Properties")
            .check(classes)
    }

    @Test
    fun `리포지토리는 infrastructure repository 패키지의 인터페이스다`() {
        classes().that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("$INFRASTRUCTURE.*.repository").andShould().beInterfaces()
            .check(classes)
    }

    @Test
    fun `포트 어댑터 유스케이스 이름을 쓰지 않는다`() {
        noClasses().should().haveSimpleNameEndingWith("Port")
            .orShould().haveSimpleNameEndingWith("Adapter")
            .orShould().haveSimpleNameContaining("UseCase")
            .check(classes)
    }

    /** 생성자 매개변수와 필드 중에 같은 역할(애노테이션)의 다른 클래스가 있으면 위반이다. */
    private fun notInjectTypesAnnotatedWith(annotation: Class<out Annotation>): ArchCondition<JavaClass> =
        object : ArchCondition<JavaClass>("같은 역할의 다른 빈(${annotation.simpleName})을 주입받지 않는다") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                injectedTypesOf(item)
                    .filter { it != item && it.isAnnotatedWith(annotation) }
                    .forEach { events.add(SimpleConditionEvent.violated(item, "${item.name}이(가) ${it.name}을(를) 주입받는다")) }
            }
        }

    /** 계층 → 도메인(또는 global) → 역할 순서의 패키지에 있어야 한다. `application.global`만 역할 없이 둔다. */
    private fun resideInDomainRolePackage(): ArchCondition<JavaClass> =
        object : ArchCondition<JavaClass>("계층.도메인.역할 패키지에 있다") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                val segments = item.packageName.removePrefix("$ROOT.").split(".")
                val layer = segments[0]
                val domain = segments.getOrNull(1)
                val allowedRoles = if (domain == GLOBAL) GLOBAL_ROLES[layer] else DOMAIN_ROLES[layer]
                val valid = domain in DOMAINS + GLOBAL && when {
                    segments.size == 2 -> layer == "application" && domain == GLOBAL
                    segments.size == 3 -> allowedRoles.orEmpty().contains(segments[2])
                    else -> false
                }
                if (!valid) {
                    events.add(SimpleConditionEvent.violated(item, "${item.name}이(가) ${item.packageName}에 있다"))
                }
            }
        }

    private fun injectedTypesOf(item: JavaClass): Set<JavaClass> =
        item.constructors.flatMap { it.rawParameterTypes }.toSet() + item.fields.map { it.rawType }

    companion object {
        private const val ROOT = "com.santacut"
        private const val PRESENTATION = "$ROOT.presentation"
        private const val APPLICATION = "$ROOT.application"
        private const val DOMAIN = "$ROOT.domain"
        private const val INFRASTRUCTURE = "$ROOT.infrastructure"
        private const val APPLICATION_CLASS_NAME = "SantaCutApplication"
        private const val GLOBAL = "global"
        private val DOMAINS = setOf("participant", "session", "analysis", "ranking", "share", "prompt")
        private val DOMAIN_ROLES = mapOf(
            "presentation" to setOf("controller", "request", "response", "docs"),
            "application" to setOf("facade", "command", "result"),
            "domain" to setOf("model", "service"),
            "infrastructure" to setOf("repository", "client", "render"),
        )
        private val GLOBAL_ROLES = mapOf(
            "presentation" to setOf("advice", "security", "docs", "config"),
            "application" to emptySet(),
            "domain" to setOf("exception", "model", "config"),
            "infrastructure" to setOf("config"),
        )
    }
}
