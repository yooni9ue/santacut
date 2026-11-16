import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	kotlin("plugin.jpa") version "2.3.21"
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.santacut"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0"))
	implementation("org.springframework.ai:spring-ai-starter-model-google-genai")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")
	implementation("org.hibernate.orm:hibernate-community-dialects")
	implementation("org.xerial:sqlite-jdbc:3.51.1.0")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property", "-Xemit-jvm-type-annotations")
	}
}

// 로컬 실행에서는 dev처럼 프롬프트 관리 API를 켠다. 운영 이미지(bootJar)의 기본값은 꺼짐이다.
tasks.named<BootRun>("bootRun") {
	systemProperty("santacut.prompt-admin.enabled", "true")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// 통합 테스트가 매번 빈 데이터베이스에서 시작하도록 이전 테스트 DB 파일을 지운다.
	val testDatabase = layout.buildDirectory.file("test.db")
	doFirst { testDatabase.get().asFile.delete() }
	// 테스트는 Gemini를 부르지 않는다(가짜 ChatModel). 클라이언트 빈을 만들 자리표시 키만 넣는다.
	environment("GEMINI_API_KEY", "test-placeholder-not-a-real-key")
}




