
import io.freefair.gradle.plugins.aspectj.AspectjCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.spring") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.jpa") version "2.3.0"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "9.1.0"
    id("io.freefair.aspectj.post-compile-weaving") version "9.1.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("maven-publish")
    signing
}
group = "dev.tommasop1804"
version = "1.2.3"
// Spring-Utils
// Tommaso Pastorelli
// Last update: Tommaso Pastorelli | 20260219T184646Z

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // Spring Boot starter dependency
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jboss:jandex:3.1.7")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.slf4j:slf4j-api:2.0.9")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // API dependencies
    api("tools.jackson.dataformat:jackson-dataformat-yaml")
    api("tools.jackson.module:jackson-module-kotlin")

    // Compile-only dependency
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    // AspectJ dependencies
    implementation("org.aspectj:aspectjrt:1.9.24")
    implementation("org.aspectj:aspectjweaver:1.9.24")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.slf4j:jul-to-slf4j:2.0.13")
    aspect("dev.tommasop1804:kotlin-utils:1.0.0")
    implementation("dev.tommasop1804:kotlin-utils:1.0.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xallow-contracts-on-more-functions",
            "-Xcontext-parameters"
        )
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xallow-contracts-on-more-functions")
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xallow-holdsin-contract")
        freeCompilerArgs.add("-Xallow-condition-implies-returns-contracts")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType(AspectjCompile::class.java).configureEach {
    options.compilerArgs.add("-Xlint:ignore")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions"
        ))
    }
}

mavenPublishing {
    coordinates("dev.tommasop1804", "spring-utils", "1.2.3")

    pom {
        name.set("Spring Utils")
        description.set("Utility functions and annotations for Spring × Kotlin")
        inceptionYear.set("2026")
        url.set("https://github.com/TommasoP1804")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("tommasop1804")
                name.set("Tommaso Pastorelli")
                url.set("https://tommasop1804.dev")
            }
        }
        scm {
            url.set("https://github.com/tommasop1804/spring-utils")
            connection.set("scm:git:git://github.com/tommasop1804/spring-utils.git")
            developerConnection.set("scm:git:ssh://git@github.com/tommasop1804/spring-utils.git")
        }
    }

    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
