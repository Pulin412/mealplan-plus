plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

group = "com.mealplanplus"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic:1.0.0")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama:1.0.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Firebase JWT validation (JWKS/RS256, no Admin SDK)
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")

    // Web Push (VAPID) for PWA reminders — standards-based, NOT Firebase FCM (guardrail-safe).
    // web-push uses Apache HttpClient 4 (transitive) but does NOT bundle BouncyCastle, which it
    // needs for the EC/JOSE crypto — so add the BC provider explicitly.
    implementation("nl.martijndwars:web-push:5.1.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Observability
    // Structured JSON logs on the prod profile → Cloud Logging parses stdout JSON (see logback-spring.xml).
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    // Backend error tracking. Dormant until SENTRY_DSN is set (free tier); empty DSN = SDK disabled.
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.14.0")

    // DB drivers
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // Flyway (PostgreSQL migrations — disabled for H2 dev profile)
    implementation("org.flywaydb:flyway-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    // Real-Postgres integration tests (versions from the Spring Boot BOM).
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

// ── OpenAPI code generation ───────────────────────────────────────────────────
// Run the generator CLI in an isolated JVM so it never shares Spring Boot's
// Jackson classpath (which would cause NoSuchMethodError on GeneratorBase).
val openApiCli: Configuration by configurations.creating {
    isTransitive = true
}

dependencies {
    openApiCli("org.openapitools:openapi-generator-cli:7.9.0")
}

val openApiGenerateDir = layout.buildDirectory.dir("generated/openapi")

tasks.register<JavaExec>("openApiGenerate") {
    group = "openapi"
    description = "Generate Spring Boot API interfaces and models from docs/openapi.yaml"
    classpath = openApiCli
    mainClass.set("org.openapitools.codegen.OpenAPIGenerator")
    args = listOf(
        "generate",
        "-g", "kotlin-spring",
        "-i", "${projectDir}/../docs/openapi.yaml",
        "-o", openApiGenerateDir.get().asFile.absolutePath,
        "--api-package",   "com.mealplanplus.api.generated.api",
        "--model-package", "com.mealplanplus.api.generated.model",
        "--additional-properties",
            "interfaceOnly=true,useSpringBoot3=true,useTags=true," +
            "gradleBuildFile=false,exceptionHandler=false," +
            "skipDefaultInterface=true,useResponseEntity=false," +
            "useBeanValidation=true,documentationProvider=none",
        "--type-mappings",   "DateTime=Instant",
        "--import-mappings", "Instant=java.time.Instant",
        "--skip-validate-spec",
    )
    outputs.dir(openApiGenerateDir)
    inputs.file("${projectDir}/../docs/openapi.yaml")
}

sourceSets {
    main {
        kotlin {
            srcDir(openApiGenerateDir.map { it.dir("src/main/kotlin") })
        }
    }
}

tasks.compileKotlin {
    dependsOn(tasks.named("openApiGenerate"))
}

// ─────────────────────────────────────────────────────────────────────────────

tasks.bootJar {
    archiveFileName.set("backend.jar")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
